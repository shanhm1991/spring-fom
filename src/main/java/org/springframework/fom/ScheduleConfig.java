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

import org.springframework.fom.quartz.CronExpression;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ScheduleConfig {
	
	/**
	 * 加载时是否启动
	 */
	public static final String KEY_enable = "enable";
	
	/**
	 * 启动时是否执行
	 */
	public static final String KEY_execOnLoad = "execOnLoad";
	
	/**
	 * 定时计划：cron
	 */
	public static final String KEY_cron = "cron";
	
	/**
	 * 定时计划：fixedRate
	 */
	public static final String KEY_fixedRate = "fixedRate";

	/**
	 * 定时计划：fixedDelay
	 */
	public static final String KEY_fixedDelay = "fixedDelay";
	
	/**
	 * 线程池任务队列长度
	 */
	public static final String KEY_queueSize = "queueSize";

	/**
	 * 线程池核心线程数
	 */
	public static final String KEY_threadCore = "threadCore";

	/**
	 * 线程池最大线程数
	 */
	public static final String KEY_threadMax = "threadMax";

	/**
	 * 线程池任务线程最长空闲时间
	 */
	public static final String KEY_threadAliveTime = "threadAliveTime";
	
	/**
	 * 任务超时时间
	 */
	public static final String KEY_taskOverTime = "taskOverTime";
	
	/**
	 * 是否对每个任务单独检测超时
	 */
	public static final String KEY_detectTimeoutOnEachTask = "detectTimeoutOnEachTask";
	
	/**
	 * 是否检测任务冲突
	 */
	public static final String KEY_enableTaskConflict = "enableTaskConflict";
	
	/**
	 * Running时是否忽略执行请求
	 */
	public static final String KEY_ignoreExecRequestWhenRunning = "ignoreExecRequestWhenRunning";
	
	/**
	 * 备注
	 */
	public static final String KEY_remark = "remark";
	
	/**
	 * 首次执行延迟时间
	 */
	public static final String KEY_initialDelay = "initialDelay";
	
	/**
	 * 任务截止时间
	 */
	public static final String KEY_deadTime = "deadTime";
	
	/**
	 * 默认加载启动
	 */
	public static final boolean DEFAULT_enable = true;
	
	/**
	 * 启动时默认不执行
	 */
	public static final boolean DEFAULT_execOnLoad = false;
	
	/**
	 * fixedRate 默认0
	 */
	public static final int DEFAULT_fixedRate = 0;
	
	/**
	 * fixedDelay 默认0
	 */
	public static final int DEFAULT_fixedDelay = 0;
	
	/**
	 * 线程数 默认1
	 */
	public static final int DEFAULT_threadCore = 1;
	
	/**
	 * 线程空闲时间：默认1
	 */
	public static final int DEFAULT_threadAliveTime = 10;
	
	/**
	 * 任务队列长度：默认256
	 */
	public static final int DEFAULT_queueSize = 256;
	
	/**
	 * 任务超时时间：默认不超时
	 */
	public static final int DEFAULT_taskOverTime = 0;
	
	/**
	 * 默认不检测任务冲突
	 */
	public static final boolean DEFAULT_enableTaskConflict = false;
	
	/**
	 * 默认对每个任务单独检测超时
	 */
	public static final boolean DEFAULT_detectTimeoutOnEachTask = true;
	
	/**
	 * Running状态时默认忽略执行请求
	 */
	public static final boolean DEFAULT_ignoreExecRequestWhenRunning = true;
	
	/**
	 * 首次执行延迟时间 默认0
	 */
	public static final long DEFAULT_initialDelay = 0;
	
	/**
	 * 任务截止时间 默认0
	 */
	public static final long DEFAULT_deadTime = 0;
	
	// 内部配置，不允许直接put
	private static Map<String, Object> internalConf = new TreeMap<>();

	private final Map<String, List<Field>> envirmentConf = new HashMap<>();

	static{
		internalConf.put(KEY_cron, "");
		internalConf.put(KEY_fixedRate,  DEFAULT_fixedRate);
		internalConf.put(KEY_fixedDelay, DEFAULT_fixedDelay);
		internalConf.put(KEY_remark, "");
		internalConf.put(KEY_queueSize,  DEFAULT_queueSize);
		internalConf.put(KEY_threadCore, DEFAULT_threadCore);
		internalConf.put(KEY_threadMax,       DEFAULT_threadCore);
		internalConf.put(KEY_threadAliveTime, DEFAULT_threadAliveTime);
		internalConf.put(KEY_taskOverTime,    DEFAULT_taskOverTime);
		internalConf.put(KEY_execOnLoad,      DEFAULT_execOnLoad);
		internalConf.put(KEY_enableTaskConflict,           DEFAULT_enableTaskConflict);
		internalConf.put(KEY_detectTimeoutOnEachTask,      DEFAULT_detectTimeoutOnEachTask);
		internalConf.put(KEY_ignoreExecRequestWhenRunning, DEFAULT_ignoreExecRequestWhenRunning);
		internalConf.put(KEY_enable,       DEFAULT_enable);
		internalConf.put(KEY_initialDelay, DEFAULT_initialDelay);
		internalConf.put(KEY_deadTime, DEFAULT_deadTime);
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

	Map<String, List<Field>> getEnvirment() {
		return envirmentConf;
	}

	TimedExecutorPool getPool() {
		return pool;
	}

	public Map<String, Object> getConfMap() {
		Map<String, Object> map = new HashMap<>();
		map.putAll(confMap);
		map.put(KEY_cron, getCronExpression());
		return map;
	} 

	Map<String, Object> getOriginalMap() {
		return confMap;
	}
	
	void copy(ScheduleConfig scheduleConfig) {
		confMap.putAll(scheduleConfig.confMap);
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
		return (CronExpression)confMap.get(KEY_cron);
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
			confMap.put(KEY_cron, cronExpression);
			return true;
		}
		return false;
	}

	public long getFixedRate(){
		return MapUtils.getLongValue(confMap, KEY_fixedRate, DEFAULT_fixedRate);
	}

	public boolean setFixedRate(long fixedRate){
		if(fixedRate > DEFAULT_fixedRate && fixedRate != getFixedRate()){
			confMap.put(KEY_fixedRate, fixedRate);
			return true;
		}
		return false;
	}

	public long getFixedDelay(){
		return MapUtils.getLongValue(confMap, KEY_fixedDelay, DEFAULT_fixedDelay);
	}

	public boolean setFixedDelay(long fixedDelay){
		if(fixedDelay > DEFAULT_fixedDelay && fixedDelay != getFixedDelay()){
			confMap.put(KEY_fixedDelay, fixedDelay);
			return true;
		}
		return false;
	}

	public String getRemark(){
		return MapUtils.getString(confMap, KEY_remark, "");
	}

	public boolean setRemark(String remark){
		if(remark.equals(getRemark())){
			return false;
		}
		confMap.put(KEY_remark, remark);
		return true;
	}

	public int getThreadCore(){
		return MapUtils.getIntValue(confMap, KEY_threadCore, DEFAULT_threadCore);
	}

	public boolean setThreadCore(int threadCore){
		Assert.isTrue(threadCore >= DEFAULT_threadCore,
				buildMsg(KEY_threadCore, " cannot be less than ", DEFAULT_threadCore));
		if(threadCore == getThreadCore()){
			return false;
		}

		confMap.put(KEY_threadCore, threadCore);
		int threadMax = getThreadMax();
		if(threadMax < threadCore){ 
			threadMax = threadCore;
			confMap.put(KEY_threadMax, threadMax);
		}

		if(pool != null && pool.getCorePoolSize() != threadCore){
			pool.setCorePoolSize(threadCore);
			pool.setMaximumPoolSize(threadMax); 
		}
		return true;
	}

	public int getThreadMax(){
		return MapUtils.getIntValue(confMap, KEY_threadMax, DEFAULT_threadCore);
	}

	public boolean setThreadMax(int threadMax){
		Assert.isTrue(threadMax >= DEFAULT_threadCore,
				buildMsg(KEY_threadMax, " cannot be less than ", DEFAULT_threadCore));
		if(threadMax == getThreadMax()){
			return false;
		}

		confMap.put(KEY_threadMax, threadMax);
		int threadCore = getThreadCore();
		if(threadCore > threadMax){
			threadCore = threadMax;
			confMap.put(KEY_threadCore, threadCore);
		}

		if(pool != null && pool.getMaximumPoolSize() != threadMax){
			pool.setCorePoolSize(threadCore);
			pool.setMaximumPoolSize(threadMax);
		}
		return true;
	}

	public int getThreadAliveTime(){
		return MapUtils.getIntValue(confMap, KEY_threadAliveTime, DEFAULT_threadAliveTime);
	}

	public boolean setThreadAliveTime(int aliveTime){
		Assert.isTrue(aliveTime >= DEFAULT_threadAliveTime,
				buildMsg(KEY_threadAliveTime, " cannot be less than ", DEFAULT_threadAliveTime));
		if(aliveTime == getThreadAliveTime()){
			return false;
		}
		confMap.put(KEY_threadAliveTime, aliveTime);
		if(pool != null && pool.getKeepAliveTime(TimeUnit.MILLISECONDS) != aliveTime){ 
			pool.setKeepAliveTime(aliveTime, TimeUnit.MILLISECONDS);
		}
		return true;
	}

	public int getTaskOverTime(){
		return MapUtils.getIntValue(confMap, DEFAULT_taskOverTime);
	}

	public boolean setTaskOverTime(int overTime){
		Assert.isTrue(overTime == DEFAULT_taskOverTime|| 
				overTime > 1000, buildMsg(KEY_taskOverTime, " cannot be less than 1000"));
		if(overTime == getTaskOverTime()){
			return false;
		}
		confMap.put(KEY_taskOverTime, overTime);
		return true;
	}

	public boolean getExecOnLoad(){
		return MapUtils.getBoolean(confMap, KEY_execOnLoad, DEFAULT_execOnLoad);
	}

	public boolean setExecOnLoad(boolean execOnLoad) {
		if(execOnLoad == getExecOnLoad()){
			return false;
		}
		confMap.put(KEY_execOnLoad, execOnLoad);
		return true;
	}

	public boolean getDetectTimeoutOnEachTask(){
		return MapUtils.getBoolean(confMap, KEY_detectTimeoutOnEachTask, DEFAULT_detectTimeoutOnEachTask);
	}

	public boolean setDetectTimeoutOnEachTask(boolean detectTimeoutOnEachTask){
		if(detectTimeoutOnEachTask == getDetectTimeoutOnEachTask()){
			return false;
		}
		confMap.put(KEY_detectTimeoutOnEachTask, detectTimeoutOnEachTask);
		return true;
	}

	public boolean getEnableTaskConflict(){
		return MapUtils.getBoolean(confMap, KEY_enableTaskConflict, DEFAULT_enableTaskConflict);
	}

	public boolean setEnableTaskConflict(boolean enableTaskConflict){
		if(enableTaskConflict == getEnableTaskConflict()){
			return false;
		}
		confMap.put(KEY_enableTaskConflict, enableTaskConflict);
		return true;
	}
	
	public boolean getEnable(){
		return MapUtils.getBoolean(confMap, KEY_enable, DEFAULT_enable);
	}
	
	public boolean setEnable(boolean enable){
		if(enable == getEnable()){
			return false;
		}
		confMap.put(KEY_enable, enable);
		return true;
	}
	
	public long getInitialDelay() {
		return MapUtils.getLong(confMap, KEY_initialDelay, DEFAULT_initialDelay);
	}
	
	public boolean setInitialDelay(long initialDelay) {
		if(initialDelay == getInitialDelay()) {
			return false;
		}
		confMap.put(KEY_initialDelay, initialDelay);
		return true;
	}
	
	public long getDeadTime() {
		return MapUtils.getLong(confMap, KEY_deadTime, DEFAULT_deadTime); // TODO
	}
	
	public boolean setDeadTime(long deadTime) {
		if(deadTime == getDeadTime()) {
			return false;
		}
		confMap.put(KEY_deadTime, deadTime);
		return true;
	}

	public boolean getIgnoreExecRequestWhenRunning(){
		return MapUtils.getBoolean(confMap, KEY_ignoreExecRequestWhenRunning, DEFAULT_ignoreExecRequestWhenRunning);
	}

	public boolean setIgnoreExecRequestWhenRunning(boolean ignoreExecRequestWhenRunning){
		if(ignoreExecRequestWhenRunning == getIgnoreExecRequestWhenRunning()){
			return false;
		}
		confMap.put(KEY_ignoreExecRequestWhenRunning, ignoreExecRequestWhenRunning);
		return true;
	}

	public int getQueueSize(){
		return MapUtils.getIntValue(confMap, KEY_queueSize, DEFAULT_queueSize);
	}

	boolean setQueueSize(int queueSize){
		Assert.isTrue(queueSize >= 1, buildMsg(KEY_queueSize, " cannot be less than 1"));
		if(queueSize == getQueueSize()){
			return false;
		}
		confMap.put(KEY_queueSize, queueSize);
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
		case KEY_cron:
			setCron(value.toString()); return;
		case KEY_fixedRate:
			setFixedRate(Long.valueOf(value.toString())); return;
		case KEY_fixedDelay:
			setFixedDelay(Long.valueOf(value.toString())); return;
		case KEY_remark:
			setRemark(value.toString()); return;
		case KEY_threadCore:
			setThreadCore(Integer.valueOf(value.toString())); return;
		case KEY_threadMax:
			setThreadMax(Integer.valueOf(value.toString())); return;
		case KEY_threadAliveTime:
			setThreadAliveTime(Integer.valueOf(value.toString())); return;
		case KEY_taskOverTime:
			setTaskOverTime(Integer.valueOf(value.toString())); return;
		case KEY_detectTimeoutOnEachTask:
			setDetectTimeoutOnEachTask(Boolean.valueOf(value.toString())); return; 
		case KEY_ignoreExecRequestWhenRunning:
			setIgnoreExecRequestWhenRunning(Boolean.valueOf(value.toString())); return; 
		case KEY_enableTaskConflict:
			setEnableTaskConflict(Boolean.valueOf(value.toString())); return; 
		case KEY_queueSize:
			setQueueSize(Integer.valueOf(value.toString())); return;  
		case KEY_execOnLoad:
			setExecOnLoad(Boolean.valueOf(value.toString())); return;  
		case KEY_enable:
			setEnable(Boolean.valueOf(value.toString())); return;  
		default:
			throw new UnsupportedOperationException("config[" + key + "] cannot be change");
		}
	}
}
