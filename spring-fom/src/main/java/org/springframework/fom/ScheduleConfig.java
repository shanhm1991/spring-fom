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

import org.quartz.CronExpression;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.fom.collections.MapUtils;
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
		internalConf.put(FomSchedule.CRON, "");
		internalConf.put(FomSchedule.FIXED_RATE, FomSchedule.FIXED_RATE_DEFAULT);
		internalConf.put(FomSchedule.FIXED_DELAY, FomSchedule.FIXED_DELAY_DEFAULT);
		internalConf.put(FomSchedule.REMARK, "");
		internalConf.put(FomSchedule.QUEUE_SIZE, FomSchedule.QUEUE_SIZE_DEFAULT);
		internalConf.put(FomSchedule.THREAD_CORE, FomSchedule.THREAD_CORE_DEFAULT);
		internalConf.put(FomSchedule.THREAD_MAX, FomSchedule.THREAD_CORE_DEFAULT);
		internalConf.put(FomSchedule.THREAD_ALIVETIME, FomSchedule.THREAD_ALIVETIME_DEFAULT);
		internalConf.put(FomSchedule.TASK_OVERTIME, FomSchedule.TASK_OVERTIME_DEFAULT);
		internalConf.put(FomSchedule.EXEC_ONLOAN, FomSchedule.EXEC_ONLOAN_DEFAULT);
		internalConf.put(FomSchedule.ENABLE_TASKRESULT_STAT, FomSchedule.ENABLE_TASKRESULT_STAT_DEFAULT); 
		internalConf.put(FomSchedule.ENABLE_TASK_CONFLICT, FomSchedule.ENABLE_TASK_CONFLICT_DEFAULT);
		internalConf.put(FomSchedule.CANCEL_TASK_ONTIMEOUT, FomSchedule.CANCEL_TASK_ONTIMEOUT_DEFAULT);
		internalConf.put(FomSchedule.DETECT_TIMEOUT_ONEACHTASK, FomSchedule.DETECT_TIMEOUT_ONEACHTASK_DEFAULT);
		internalConf.put(FomSchedule.IGNORE_EXECREQUEST_WHEN_RUNNING, FomSchedule.IGNORE_EXECREQUEST_WHEN_RUNNING_DEFAULT);

		//readOnlyConf.add(FomSchedule.QUEUE_SIZE);
		//readOnlyConf.add(FomSchedule.EXEC_ONLOAN);
	}

	private final ConcurrentHashMap<String, Object> confMap = new ConcurrentHashMap<>();

	private TimedExecutorPool pool;

	public void reset(){
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
		map.put(FomSchedule.CRON, getCronExpression());
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
				map.put(future.getTaskId(), format.format(future.getCreateTime()));
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
		return (CronExpression)confMap.get(FomSchedule.CRON);
	}

	public String getCronExpression(){
		CronExpression cron = getCron();
		if(cron != null){
			return cron.getCronExpression();
		}
		return null;
	}

	public boolean setCron(String cron){
		if(StringUtils.isEmpty(cron)){
			return false;
		}

		CronExpression cronExpression;
		try {
			cronExpression = new CronExpression(cron);
		} catch (ParseException e) {
			throw new IllegalArgumentException("cronExpression cannot parsed", e);
		}

		if(!cron.equals(getCron())){
			confMap.put(FomSchedule.CRON, cronExpression);
			return true;
		}
		return false;
	}

	public long getFixedRate(){
		return MapUtils.getLongValue(confMap, FomSchedule.FIXED_RATE, FomSchedule.FIXED_RATE_DEFAULT);
	}

	public boolean setFixedRate(long fixedRate){
		if(fixedRate > FomSchedule.FIXED_RATE_DEFAULT && fixedRate != getFixedRate()){
			confMap.put(FomSchedule.FIXED_RATE, fixedRate);
			return true;
		}
		return false;
	}

	public long getFixedDelay(){
		return MapUtils.getLongValue(confMap, FomSchedule.FIXED_DELAY, FomSchedule.FIXED_DELAY_DEFAULT);
	}

	public boolean setFixedDelay(long fixedDelay){
		if(fixedDelay > FomSchedule.FIXED_DELAY_DEFAULT && fixedDelay != getFixedDelay()){
			confMap.put(FomSchedule.FIXED_DELAY, fixedDelay);
			return true;
		}
		return false;
	}

	public String getRemark(){
		return MapUtils.getString(confMap, FomSchedule.REMARK, "");
	}

	public boolean setRemark(String remark){
		if(remark.equals(getRemark())){
			return false;
		}
		confMap.put(FomSchedule.REMARK, remark);
		return true;
	}

	public int getThreadCore(){
		return MapUtils.getIntValue(confMap, FomSchedule.THREAD_CORE, FomSchedule.THREAD_CORE_DEFAULT);
	}

	public boolean setThreadCore(int threadCore){
		Assert.isTrue(threadCore >= FomSchedule.THREAD_CORE_DEFAULT, 
				buildMsg(FomSchedule.THREAD_CORE, " cannot be less than ", FomSchedule.THREAD_CORE_DEFAULT)); 
		if(threadCore == getThreadCore()){
			return false;
		}

		confMap.put(FomSchedule.THREAD_CORE, threadCore);
		int threadMax = getThreadMax();
		if(threadMax < threadCore){ 
			threadMax = threadCore;
			confMap.put(FomSchedule.THREAD_MAX, threadMax);
		}

		if(pool != null && pool.getCorePoolSize() != threadCore){
			pool.setCorePoolSize(threadCore);
			pool.setMaximumPoolSize(threadMax); 
		}
		return true;
	}

	public int getThreadMax(){
		return MapUtils.getIntValue(confMap, FomSchedule.THREAD_MAX, FomSchedule.THREAD_CORE_DEFAULT);
	}

	public boolean setThreadMax(int threadMax){
		Assert.isTrue(threadMax >= FomSchedule.THREAD_CORE_DEFAULT, 
				buildMsg(FomSchedule.THREAD_MAX, " cannot be less than ",FomSchedule. THREAD_CORE_DEFAULT)); 
		if(threadMax == getThreadMax()){
			return false;
		}

		confMap.put(FomSchedule.THREAD_MAX, threadMax);
		int threadCore = getThreadCore();
		if(threadCore > threadMax){
			threadCore = threadMax;
			confMap.put(FomSchedule.THREAD_CORE, threadCore);
		}

		if(pool != null && pool.getMaximumPoolSize() != threadMax){
			pool.setCorePoolSize(threadCore);
			pool.setMaximumPoolSize(threadMax);
		}
		return true;
	}

	public int getThreadAliveTime(){
		return MapUtils.getIntValue(confMap, FomSchedule.THREAD_ALIVETIME, FomSchedule.THREAD_ALIVETIME_DEFAULT);
	}

	public boolean setThreadAliveTime(int aliveTime){
		Assert.isTrue(aliveTime >= FomSchedule.THREAD_ALIVETIME_DEFAULT, 
				buildMsg(FomSchedule.THREAD_ALIVETIME, " cannot be less than ", FomSchedule.THREAD_ALIVETIME_DEFAULT)); 
		if(aliveTime == getThreadAliveTime()){
			return false;
		}
		confMap.put(FomSchedule.THREAD_ALIVETIME, aliveTime);
		if(pool != null && pool.getKeepAliveTime(TimeUnit.SECONDS) != aliveTime){ 
			pool.setKeepAliveTime(aliveTime, TimeUnit.SECONDS);
		}
		return true;
	}

	public int getTaskOverTime(){
		return MapUtils.getIntValue(confMap, FomSchedule.TASK_OVERTIME, FomSchedule.TASK_OVERTIME_DEFAULT);
	}

	public boolean setTaskOverTime(int overTime){
		Assert.isTrue(overTime >= FomSchedule.TASK_OVERTIME_DEFAULT, 
				buildMsg(FomSchedule.TASK_OVERTIME, " cannot be less than ", FomSchedule.TASK_OVERTIME_DEFAULT)); 
		if(overTime == getTaskOverTime()){
			return false;
		}
		confMap.put(FomSchedule.TASK_OVERTIME, overTime);
		return true;
	}

	public boolean getExecOnLoad(){
		return MapUtils.getBoolean(confMap, FomSchedule.EXEC_ONLOAN, FomSchedule.EXEC_ONLOAN_DEFAULT);
	}

	public boolean setExecOnLoad(boolean execOnLoad) {
		if(execOnLoad == getExecOnLoad()){
			return false;
		}
		confMap.put(FomSchedule.EXEC_ONLOAN, execOnLoad);
		return true;
	}

	public boolean getCancelTaskOnTimeout(){
		return MapUtils.getBoolean(confMap, FomSchedule.CANCEL_TASK_ONTIMEOUT, FomSchedule.CANCEL_TASK_ONTIMEOUT_DEFAULT);
	}

	public boolean setCancelTaskOnTimeout(boolean cancelTaskOnTimeout){
		if(cancelTaskOnTimeout == getCancelTaskOnTimeout()){
			return false;
		}
		confMap.put(FomSchedule.CANCEL_TASK_ONTIMEOUT, cancelTaskOnTimeout);
		return true;
	}

	public boolean getDetectTimeoutOnEachTask(){
		return MapUtils.getBoolean(confMap, FomSchedule.DETECT_TIMEOUT_ONEACHTASK, FomSchedule.DETECT_TIMEOUT_ONEACHTASK_DEFAULT);
	}

	public boolean setDetectTimeoutOnEachTask(boolean detectTimeoutOnEachTask){
		if(detectTimeoutOnEachTask == getDetectTimeoutOnEachTask()){
			return false;
		}
		confMap.put(FomSchedule.DETECT_TIMEOUT_ONEACHTASK, detectTimeoutOnEachTask);
		return true;
	}

	public boolean getEnableTaskResultStat(){ 
		return MapUtils.getBoolean(confMap, FomSchedule.ENABLE_TASKRESULT_STAT, FomSchedule.ENABLE_TASKRESULT_STAT_DEFAULT);
	}

	public boolean setEnableTaskResultStat(boolean enableTaskResultStat){
		if(enableTaskResultStat == getEnableTaskResultStat()){
			return false;
		}
		confMap.put(FomSchedule.ENABLE_TASKRESULT_STAT, enableTaskResultStat);
		return true;
	}

	public boolean getEnableTaskConflict(){
		return MapUtils.getBoolean(confMap, FomSchedule.ENABLE_TASK_CONFLICT, FomSchedule.ENABLE_TASK_CONFLICT_DEFAULT);
	}

	public boolean setEnableTaskConflict(boolean enableTaskConflict){
		if(enableTaskConflict == getEnableTaskConflict()){
			return false;
		}
		confMap.put(FomSchedule.ENABLE_TASK_CONFLICT, enableTaskConflict);
		return true;
	}

	public boolean getIgnoreExecRequestWhenRunning(){
		return MapUtils.getBoolean(confMap, FomSchedule.IGNORE_EXECREQUEST_WHEN_RUNNING, FomSchedule.IGNORE_EXECREQUEST_WHEN_RUNNING_DEFAULT);
	}

	public boolean setIgnoreExecRequestWhenRunning(boolean ignoreExecRequestWhenRunning){
		if(ignoreExecRequestWhenRunning == getIgnoreExecRequestWhenRunning()){
			return false;
		}
		confMap.put(FomSchedule.IGNORE_EXECREQUEST_WHEN_RUNNING, ignoreExecRequestWhenRunning);
		return true;
	}

	public int getQueueSize(){
		return MapUtils.getIntValue(confMap, FomSchedule.QUEUE_SIZE, FomSchedule.QUEUE_SIZE_DEFAULT);
	}

	boolean setQueueSize(int queueSize){
		Assert.isTrue(queueSize >= FomSchedule.QUEUE_SIZE_MIN, 
				buildMsg(FomSchedule.QUEUE_SIZE, " cannot be less than ", FomSchedule.QUEUE_SIZE_MIN)); 
		if(queueSize == getQueueSize()){
			return false;
		}
		confMap.put(FomSchedule.QUEUE_SIZE, queueSize);
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

		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss SSS");
		for(Object obj : array){
			if(obj instanceof TimedFuture){
				TimedFuture<?> future = (TimedFuture<?>)obj;
				map.put(future.getTaskId(), format.format(future.getCreateTime()));
			}
		}
		return map;
	}

	List<Map<String, String>> getActiveTasks(){
		List<Map<String, String>> list = new ArrayList<>();
		if(pool == null){
			return list;
		}

		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		for(Entry<Task<?>, Thread> entry : pool.getActiveThreads().entrySet()){
			Task<?> task = entry.getKey();
			Thread thread = entry.getValue();

			Map<String, String> map = new HashMap<>();
			map.put("id", task.getTaskId());
			map.put("createTime", format.format(task.getCreateTime()));
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

	// 没有做自我保护
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
				confMap.put(key, value);
			}
		}
		return envirmentFieldChange;
	}

	// 配置有限，这里简单处理下，这样做主要是为了自我保护，防止配置被恶意修改
	private void saveInternalConfig(String key, Object value){
		switch(key){
		case FomSchedule.CRON:
			setCron(value.toString()); return;
		case FomSchedule.FIXED_RATE:
			setFixedRate(Long.valueOf(value.toString())); return;
		case FomSchedule.FIXED_DELAY:
			setFixedDelay(Long.valueOf(value.toString())); return;
		case FomSchedule.REMARK:
			setRemark(value.toString()); return;
		case FomSchedule.THREAD_CORE:
			setThreadCore(Integer.valueOf(value.toString())); return;
		case FomSchedule.THREAD_MAX:
			setThreadMax(Integer.valueOf(value.toString())); return;
		case FomSchedule.THREAD_ALIVETIME:
			setThreadAliveTime(Integer.valueOf(value.toString())); return;
		case FomSchedule.TASK_OVERTIME:
			setTaskOverTime(Integer.valueOf(value.toString())); return;
		case FomSchedule.CANCEL_TASK_ONTIMEOUT: 
			setCancelTaskOnTimeout(Boolean.valueOf(value.toString())); return; 
		case FomSchedule.DETECT_TIMEOUT_ONEACHTASK:
			setDetectTimeoutOnEachTask(Boolean.valueOf(value.toString())); return; 
		case FomSchedule.IGNORE_EXECREQUEST_WHEN_RUNNING:
			setIgnoreExecRequestWhenRunning(Boolean.valueOf(value.toString())); return; 
		case FomSchedule.ENABLE_TASK_CONFLICT:
			setEnableTaskConflict(Boolean.valueOf(value.toString())); return; 
		case FomSchedule.ENABLE_TASKRESULT_STAT: 
			setEnableTaskResultStat(Boolean.valueOf(value.toString())); return;  
		case FomSchedule.QUEUE_SIZE:
			setQueueSize(Integer.valueOf(value.toString())); return;  
		case FomSchedule.EXEC_ONLOAN:
			setExecOnLoad(Boolean.valueOf(value.toString())); return;  
		default:
			throw new UnsupportedOperationException("config[" + key + "] cannot be change");
		}
	}
}
