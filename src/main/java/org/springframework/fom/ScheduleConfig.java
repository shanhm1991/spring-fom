package org.springframework.fom;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.fom.annotation.Fom;
import org.springframework.fom.collections.MapUtils;
import org.springframework.fom.quartz.CronExpression;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ScheduleConfig {

	// 内定配置，不允许直接put
	private static Map<String, Object> internalConf = new TreeMap<>();

	// 暂时不用，所有配置都可以修改，只是生效时机不同
	private static Set<String> readOnlyConf = new HashSet<>();

	private final Map<String, List<Field>> envirmentConf = new HashMap<>();

	static{
		internalConf.put(Fom.CRON, "");
		internalConf.put(Fom.FIXED_RATE, Fom.FIXED_RATE_DEFAULT);
		internalConf.put(Fom.FIXED_DELAY, Fom.FIXED_DELAY_DEFAULT);
		internalConf.put(Fom.REMARK, "");
		internalConf.put(Fom.QUEUE_SIZE, Fom.QUEUE_SIZE_DEFAULT);
		internalConf.put(Fom.THREAD_CORE, Fom.THREAD_CORE_DEFAULT);
		internalConf.put(Fom.THREAD_MAX, Fom.THREAD_CORE_DEFAULT);
		internalConf.put(Fom.THREAD_ALIVETIME, Fom.THREAD_ALIVETIME_DEFAULT);
		internalConf.put(Fom.TASK_OVERTIME, Fom.TASK_OVERTIME_DEFAULT);
		internalConf.put(Fom.EXEC_ONLOAN, Fom.EXEC_ONLOAN_DEFAULT);
		internalConf.put(Fom.ENABLE_TASK_CONFLICT, Fom.ENABLE_TASK_CONFLICT_DEFAULT);
		internalConf.put(Fom.DETECT_TIMEOUT_ONEACHTASK, Fom.DETECT_TIMEOUT_ONEACHTASK_DEFAULT);
		internalConf.put(Fom.IGNORE_EXECREQUEST_WHEN_RUNNING, Fom.IGNORE_EXECREQUEST_WHEN_RUNNING_DEFAULT);
		internalConf.put(Fom.ENABLE, Fom.ENABLE_DEFAULT);
		//readOnlyConf.add(FomSchedule.QUEUE_SIZE);
		//readOnlyConf.add(FomSchedule.EXEC_ONLOAN);
	}

	private final ConcurrentHashMap<String, Object> confMap = new ConcurrentHashMap<>();

	private TimedExecutorPool pool;

	public void refresh(){
		int core = getThreadCore(); 
		int max = getThreadMax();
		int aliveTime = getThreadAliveTime();   
		int queueSize = getQueueSize();  
		pool = new TimedExecutorPool(core, max, aliveTime, new LinkedBlockingQueue<Runnable>(queueSize));
		pool.allowCoreThreadTimeOut(true);
	}

	static Map<String, Object> getInternalConf() {
		return internalConf;
	}

	static Set<String> getReadOnlyConf() {
		return readOnlyConf;
	}

	Map<String, List<Field>> getEnvirment() {
		return envirmentConf;
	}

	TimedExecutorPool getPool() {
		return pool;
	}

	public Map<String, Object> getConfMap() {
		Map<String, Object> map = new HashMap<>();
		map.putAll(confMap);
		map.put(Fom.CRON, getCronExpression());
		return map;
	} 

	Map<String, Object> getOriginalMap() {
		return confMap;
	}

	public boolean containsKey(String key){
		return confMap.containsKey(key);
	}

	/** info of pool  **/
	long getActives(){
		return pool == null ? 0 : pool.getActiveCount();
	}

	int getWaitings(){
		return pool == null ? 0 : pool.getQueue().size();
	}

	long getCreated(){
		return pool == null ? 0 : pool.getTaskCount();
	}

	long getCompleted(){
		return pool == null ? 0 : pool.getCompletedTaskCount();
	}

	@SuppressWarnings("unchecked")
	Map<Task<?>, Thread> getActiveThreads() {
		return (Map<Task<?>, Thread>) (pool == null ? new HashMap<>() : pool.getActiveThreads());
	}

	@SuppressWarnings("rawtypes")
	Map<String, Object> getWaitingDetail(){
		Map<String, Object> map = new HashMap<>();
		if(pool == null){
			return map;
		}

		Object[] array = pool.getQueue().toArray();
		if(array == null || array.length == 0){
			return map;
		}

		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss SSS");
		for(Object obj : array){
			if(obj instanceof TimedFuture){
				TimedFuture future = (TimedFuture)obj;
				map.put(future.getTaskId(), format.format(future.getSubmitTime()));
			}
		}
		return map;
	}

	// get/set of config  下面的set方法其实都有先获取后判断的线程安全问题，不过对于配置更新来说，这个影响可以忽略
	public boolean set(String key, Object value) {
		if(internalConf.containsKey(key)){
			throw new UnsupportedOperationException("cannot override internal config:" + key);
		}

		if(value.equals(get(key))){
			return false;
		}
		confMap.put(key, value);
		return true;
	}

	@SuppressWarnings("unchecked")
	public <V> V get(String key){
		return (V)confMap.get(key);
	}

	public String getString(String key, String defaultValue){
		return MapUtils.getString(confMap, key, defaultValue);
	}

	public int getInt(String key, int defaultValue){
		return MapUtils.getIntValue(confMap, key, defaultValue);
	}

	public long getLong(String key, long defaultValue){
		return MapUtils.getLongValue(confMap, key, defaultValue);
	}

	public boolean getBoolean(String key, boolean defaultValue){
		return MapUtils.getBooleanValue(confMap, key, defaultValue);
	}

	private String buildMsg(Object... args){
		StringBuilder builder = new StringBuilder();
		for(Object obj : args){
			builder.append(obj);
		}
		return builder.toString();
	}

	// get/set of internal config 
	public CronExpression getCron(){
		return (CronExpression)confMap.get(Fom.CRON);
	}

	public String getCronExpression(){
		CronExpression cron = getCron();
		if(cron != null){
			return cron.getCronExpression();
		}
		return null;
	}

	public boolean setCron(String cron){
		if(!StringUtils.hasText(cron)){
			return false;
		}

		CronExpression cronExpression;
		try {
			cronExpression = new CronExpression(cron);
		} catch (ParseException e) {
			throw new IllegalArgumentException("cronExpression cannot parsed", e);
		}

		if(!cron.equals(getCron())){
			confMap.put(Fom.CRON, cronExpression);
			return true;
		}
		return false;
	}

	public long getFixedRate(){
		return MapUtils.getLongValue(confMap, Fom.FIXED_RATE, Fom.FIXED_RATE_DEFAULT);
	}

	public boolean setFixedRate(long fixedRate){
		if(fixedRate > Fom.FIXED_RATE_DEFAULT && fixedRate != getFixedRate()){
			confMap.put(Fom.FIXED_RATE, fixedRate);
			return true;
		}
		return false;
	}

	public long getFixedDelay(){
		return MapUtils.getLongValue(confMap, Fom.FIXED_DELAY, Fom.FIXED_DELAY_DEFAULT);
	}

	public boolean setFixedDelay(long fixedDelay){
		if(fixedDelay > Fom.FIXED_DELAY_DEFAULT && fixedDelay != getFixedDelay()){
			confMap.put(Fom.FIXED_DELAY, fixedDelay);
			return true;
		}
		return false;
	}

	public String getRemark(){
		return MapUtils.getString(confMap, Fom.REMARK, "");
	}

	public boolean setRemark(String remark){
		if(remark.equals(getRemark())){
			return false;
		}
		confMap.put(Fom.REMARK, remark);
		return true;
	}

	public int getThreadCore(){
		return MapUtils.getIntValue(confMap, Fom.THREAD_CORE, Fom.THREAD_CORE_DEFAULT);
	}

	public boolean setThreadCore(int threadCore){
		Assert.isTrue(threadCore >= Fom.THREAD_CORE_DEFAULT,
				buildMsg(Fom.THREAD_CORE, " cannot be less than ", Fom.THREAD_CORE_DEFAULT));
		if(threadCore == getThreadCore()){
			return false;
		}

		confMap.put(Fom.THREAD_CORE, threadCore);
		int threadMax = getThreadMax();
		if(threadMax < threadCore){ 
			threadMax = threadCore;
			confMap.put(Fom.THREAD_MAX, threadMax);
		}

		if(pool != null && pool.getCorePoolSize() != threadCore){
			pool.setCorePoolSize(threadCore);
			pool.setMaximumPoolSize(threadMax); 
		}
		return true;
	}

	public int getThreadMax(){
		return MapUtils.getIntValue(confMap, Fom.THREAD_MAX, Fom.THREAD_CORE_DEFAULT);
	}

	public boolean setThreadMax(int threadMax){
		Assert.isTrue(threadMax >= Fom.THREAD_CORE_DEFAULT,
				buildMsg(Fom.THREAD_MAX, " cannot be less than ", Fom. THREAD_CORE_DEFAULT));
		if(threadMax == getThreadMax()){
			return false;
		}

		confMap.put(Fom.THREAD_MAX, threadMax);
		int threadCore = getThreadCore();
		if(threadCore > threadMax){
			threadCore = threadMax;
			confMap.put(Fom.THREAD_CORE, threadCore);
		}

		if(pool != null && pool.getMaximumPoolSize() != threadMax){
			pool.setCorePoolSize(threadCore);
			pool.setMaximumPoolSize(threadMax);
		}
		return true;
	}

	public int getThreadAliveTime(){
		return MapUtils.getIntValue(confMap, Fom.THREAD_ALIVETIME, Fom.THREAD_ALIVETIME_DEFAULT);
	}

	public boolean setThreadAliveTime(int aliveTime){
		Assert.isTrue(aliveTime >= Fom.THREAD_ALIVETIME_DEFAULT,
				buildMsg(Fom.THREAD_ALIVETIME, " cannot be less than ", Fom.THREAD_ALIVETIME_DEFAULT));
		if(aliveTime == getThreadAliveTime()){
			return false;
		}
		confMap.put(Fom.THREAD_ALIVETIME, aliveTime);
		if(pool != null && pool.getKeepAliveTime(TimeUnit.MILLISECONDS) != aliveTime){ 
			pool.setKeepAliveTime(aliveTime, TimeUnit.MILLISECONDS);
		}
		return true;
	}

	public int getTaskOverTime(){
		return MapUtils.getIntValue(confMap, Fom.TASK_OVERTIME, Fom.TASK_OVERTIME_DEFAULT);
	}

	public boolean setTaskOverTime(int overTime){
		Assert.isTrue(overTime >= Fom.TASK_OVERTIME_DEFAULT,
				buildMsg(Fom.TASK_OVERTIME, " cannot be less than ", Fom.TASK_OVERTIME_DEFAULT));
		if(overTime == getTaskOverTime()){
			return false;
		}
		confMap.put(Fom.TASK_OVERTIME, overTime);
		return true;
	}

	public boolean getExecOnLoad(){
		return MapUtils.getBoolean(confMap, Fom.EXEC_ONLOAN, Fom.EXEC_ONLOAN_DEFAULT);
	}

	public boolean setExecOnLoad(boolean execOnLoad) {
		if(execOnLoad == getExecOnLoad()){
			return false;
		}
		confMap.put(Fom.EXEC_ONLOAN, execOnLoad);
		return true;
	}

	public boolean getDetectTimeoutOnEachTask(){
		return MapUtils.getBoolean(confMap, Fom.DETECT_TIMEOUT_ONEACHTASK, Fom.DETECT_TIMEOUT_ONEACHTASK_DEFAULT);
	}

	public boolean setDetectTimeoutOnEachTask(boolean detectTimeoutOnEachTask){
		if(detectTimeoutOnEachTask == getDetectTimeoutOnEachTask()){
			return false;
		}
		confMap.put(Fom.DETECT_TIMEOUT_ONEACHTASK, detectTimeoutOnEachTask);
		return true;
	}

	public boolean getEnableTaskConflict(){
		return MapUtils.getBoolean(confMap, Fom.ENABLE_TASK_CONFLICT, Fom.ENABLE_TASK_CONFLICT_DEFAULT);
	}

	public boolean setEnableTaskConflict(boolean enableTaskConflict){
		if(enableTaskConflict == getEnableTaskConflict()){
			return false;
		}
		confMap.put(Fom.ENABLE_TASK_CONFLICT, enableTaskConflict);
		return true;
	}
	
	public boolean getEnable(){
		return MapUtils.getBoolean(confMap, Fom.ENABLE, Fom.ENABLE_DEFAULT);
	}
	
	public boolean setEnable(boolean enable){
		if(enable == getEnable()){
			return false;
		}
		confMap.put(Fom.ENABLE, enable);
		return true;
	}

	public boolean getIgnoreExecRequestWhenRunning(){
		return MapUtils.getBoolean(confMap, Fom.IGNORE_EXECREQUEST_WHEN_RUNNING, Fom.IGNORE_EXECREQUEST_WHEN_RUNNING_DEFAULT);
	}

	public boolean setIgnoreExecRequestWhenRunning(boolean ignoreExecRequestWhenRunning){
		if(ignoreExecRequestWhenRunning == getIgnoreExecRequestWhenRunning()){
			return false;
		}
		confMap.put(Fom.IGNORE_EXECREQUEST_WHEN_RUNNING, ignoreExecRequestWhenRunning);
		return true;
	}

	public int getQueueSize(){
		return MapUtils.getIntValue(confMap, Fom.QUEUE_SIZE, Fom.QUEUE_SIZE_DEFAULT);
	}

	boolean setQueueSize(int queueSize){
		Assert.isTrue(queueSize >= Fom.QUEUE_SIZE_MIN,
				buildMsg(Fom.QUEUE_SIZE, " cannot be less than ", Fom.QUEUE_SIZE_MIN));
		if(queueSize == getQueueSize()){
			return false;
		}
		confMap.put(Fom.QUEUE_SIZE, queueSize);
		return true;
	}

	@Override
	public String toString() {
		return confMap.toString();
	}

	Map<String, String> getWaitingTasks(){
		Map<String, String> map = new HashMap<>();
		if(pool == null){
			return map;
		}
		Object[] array = pool.getQueue().toArray();
		if(array == null || array.length == 0){
			return map;
		}

		DateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss SSS");
		for(Object obj : array){
			if(obj instanceof TimedFuture){
				TimedFuture<?> future = (TimedFuture<?>)obj;
				map.put(future.getTaskId(), format.format(future.getSubmitTime()));
			}
		}
		return map;
	}

	List<Map<String, String>> getActiveTasks(){
		List<Map<String, String>> list = new ArrayList<>();
		if(pool == null){
			return list;
		}

		DateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss SSS");
		for(Entry<Task<?>, Thread> entry : pool.getActiveThreads().entrySet()){
			Task<?> task = entry.getKey();
			Thread thread = entry.getValue();

			Map<String, String> map = new HashMap<>();
			map.put("id", task.getTaskId());
			map.put("submitTime", format.format(task.getSubmitTime()));
			map.put("startTime", format.format(task.getStartTime()));

			StringBuilder builder = new StringBuilder();
			for(StackTraceElement stack : thread.getStackTrace()){
				builder.append(stack).append("<br>");
			}
			map.put("stack", builder.toString());
			list.add(map);
		}
		return list;
	}

	Set<Field> saveConfig(HashMap<String, Object> map){
		Set<Field> envirmentFieldChange = new HashSet<>();
		for(Entry<String, Object> entry : map.entrySet()){
			String key = entry.getKey();
			Object value = entry.getValue();
			if(internalConf.containsKey(key)){
				saveInternalConfig(key, value);
			}else{
				List<Field> list = envirmentConf.get(key);
				if(list != null){
					envirmentFieldChange.addAll(list);
				}
				
				// 保存配置的时候，尽量按照不改变原配置值的类型
				Object oldValue = confMap.get(key);
				if(oldValue != null){
					Class<?> clazz = oldValue.getClass();
					if(Integer.class == clazz){
						confMap.put(key, Integer.valueOf(value.toString()));
					}else if(Long.class == clazz){
						confMap.put(key, Long.valueOf(value.toString()));
					}else if(Float.class == clazz){
						confMap.put(key, Float.valueOf(value.toString()));
					}else if(Double.class == clazz){
						confMap.put(key, Double.valueOf(value.toString()));
					}else if(Boolean.class == clazz){
						confMap.put(key, Boolean.valueOf(value.toString()));
					}else if(Short.class == clazz){
						confMap.put(key, Short.valueOf(value.toString()));
					}else{
						confMap.put(key, value);
					}
				}else{
					confMap.put(key, value);
				}
			}
		}
		return envirmentFieldChange;
	}

	// 配置有限，这里简单处理下，这样做主要是为了自我保护，防止配置被恶意修改
	private void saveInternalConfig(String key, Object value){
		switch(key){
		case Fom.CRON:
			setCron(value.toString()); return;
		case Fom.FIXED_RATE:
			setFixedRate(Long.valueOf(value.toString())); return;
		case Fom.FIXED_DELAY:
			setFixedDelay(Long.valueOf(value.toString())); return;
		case Fom.REMARK:
			setRemark(value.toString()); return;
		case Fom.THREAD_CORE:
			setThreadCore(Integer.valueOf(value.toString())); return;
		case Fom.THREAD_MAX:
			setThreadMax(Integer.valueOf(value.toString())); return;
		case Fom.THREAD_ALIVETIME:
			setThreadAliveTime(Integer.valueOf(value.toString())); return;
		case Fom.TASK_OVERTIME:
			setTaskOverTime(Integer.valueOf(value.toString())); return;
		case Fom.DETECT_TIMEOUT_ONEACHTASK:
			setDetectTimeoutOnEachTask(Boolean.valueOf(value.toString())); return; 
		case Fom.IGNORE_EXECREQUEST_WHEN_RUNNING:
			setIgnoreExecRequestWhenRunning(Boolean.valueOf(value.toString())); return; 
		case Fom.ENABLE_TASK_CONFLICT:
			setEnableTaskConflict(Boolean.valueOf(value.toString())); return; 
		case Fom.QUEUE_SIZE:
			setQueueSize(Integer.valueOf(value.toString())); return;  
		case Fom.EXEC_ONLOAN:
			setExecOnLoad(Boolean.valueOf(value.toString())); return;  
		case Fom.ENABLE:
			setEnable(Boolean.valueOf(value.toString())); return;  
		default:
			throw new UnsupportedOperationException("config[" + key + "] cannot be change");
		}
	}
}
