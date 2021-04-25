package org.springframework.fom;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.MapUtils;
import org.quartz.CronExpression;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ScheduleConfig {

	/**
	 * 最小线程数：min
	 */
	public static final int THREAD_MIN = 1;

	/**
	 * 线程空闲存活时间：default
	 */
	public static final int THREAD_ALIVETIME_DEFAULT = 20;

	/**
	 * 线程空闲存活时间：min
	 */
	public static final int THREAD_ALIVETIME_MIN = 1;

	/**
	 * 任务队列长度：default
	 */
	public static final int QUEUE_SIZE_DEFAULT = 1000;

	/**
	 * 任务队列长度：min
	 */
	public static final int QUEUE_SIZE_MIN = 1;

	/**
	 * 任务超时时间：default
	 */
	public static final int TASK_OVERTIME_DEFAULT = 0;

	/**
	 * 定时计划：fixedRate 默认值：0
	 */
	public static final int FIXED_RATE_DEFAULT = 0;

	/**
	 * 定时计划：fixedDelay 默认值：0
	 */
	public static final int FIXED_DELAY_DEFAULT = 0;

	/**
	 * 启动时默认不执行
	 */
	public static final boolean EXECONLOAN_DEFAULT = false;

	/**
	 * 定时计划：cron
	 */
	public static final String CONF_CRON = "cron";

	/**
	 * 定时计划：fixedRate
	 */
	public static final String CONF_FIXEDRATE = "fixedRate";

	/**
	 * 定时计划：fixedDelay
	 */
	public static final String CONF_FIXEDDELAY = "fixedDelay";

	/**
	 * 备注
	 */
	public static final String CONF_REMARK = "remark";

	/**
	 * 线程池任务队列长度
	 */
	public static final String CONF_QUEUESIZE = "queueSize";

	/**
	 * 线程池核心线程数
	 */
	public static final String CONF_THREAD_CORE = "threadCore";

	/**
	 * 线程池最大线程数
	 */
	public static final String CONF_THREAD_MAX = "threadMax";

	/**
	 * 线程池任务线程最长空闲时间
	 */
	public static final String CONF_THREAD_ALIVETIME = "threadAliveTime";

	/**
	 * 任务超时时间
	 */
	public static final String CONF_TASK_OVERTIME = "taskOverTime";

	/**
	 * 启动时是否执行
	 */
	public static final String CONF_EXECONLOAN = "execOnLoad";

	/**
	 * 内定配置，不允许直接put
	 */
	private static Set<String> internalConf = new HashSet<>();

	static{
		internalConf.add(CONF_CRON);
		internalConf.add(CONF_FIXEDRATE);
		internalConf.add(CONF_FIXEDDELAY);
		internalConf.add(CONF_REMARK);
		internalConf.add(CONF_QUEUESIZE);
		internalConf.add(CONF_THREAD_CORE);
		internalConf.add(CONF_THREAD_MAX);
		internalConf.add(CONF_THREAD_ALIVETIME);
		internalConf.add(CONF_TASK_OVERTIME);
		internalConf.add(CONF_EXECONLOAN);
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

	TimedExecutorPool getPool() {
		return pool;
	}

	public ConcurrentHashMap<String, Object> getConfMap() {
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
		if(internalConf.contains(key)){
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
		return (CronExpression)confMap.get(CONF_CRON);
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
			confMap.put(CONF_CRON, cronExpression);
			return true;
		}
		return false;
	}

	public long getFixedRate(){
		return MapUtils.getLongValue(confMap, CONF_FIXEDRATE, FIXED_RATE_DEFAULT);
	}

	public boolean setFixedRate(long fixedRate){
		if(fixedRate > FIXED_RATE_DEFAULT && fixedRate != getFixedRate()){
			confMap.put(CONF_FIXEDRATE, fixedRate);
			return true;
		}
		return false;
	}

	public long getFixedDelay(){
		return MapUtils.getLongValue(confMap, CONF_FIXEDDELAY, FIXED_DELAY_DEFAULT);
	}

	public boolean setFixedDelay(long fixedDelay){
		if(fixedDelay > FIXED_DELAY_DEFAULT && fixedDelay != getFixedDelay()){
			confMap.put(CONF_FIXEDDELAY, fixedDelay);
			return true;
		}
		return false;
	}

	public String getRemark(){
		return MapUtils.getString(confMap, CONF_REMARK, "");
	}

	public boolean setRemark(String remark){
		if(remark.equals(getRemark())){
			return false;
		}
		confMap.put(CONF_REMARK, remark);
		return true;
	}

	public int getThreadCore(){
		return MapUtils.getIntValue(confMap, CONF_THREAD_CORE, THREAD_MIN);
	}

	public boolean setThreadCore(int threadCore){
		Assert.isTrue(threadCore >= THREAD_MIN, buildMsg(CONF_THREAD_CORE, " cannot be less than ", THREAD_MIN)); 
		if(threadCore == getThreadCore()){
			return false;
		}

		confMap.put(CONF_THREAD_CORE, threadCore);
		int threadMax = getThreadMax();
		if(threadMax < threadCore){ 
			threadMax = threadCore;
			confMap.put(CONF_THREAD_MAX, threadMax);
		}

		if(pool != null && pool.getCorePoolSize() != threadCore){
			pool.setCorePoolSize(threadCore);
			pool.setMaximumPoolSize(threadMax); 
		}
		return true;
	}

	public int getThreadMax(){
		return MapUtils.getIntValue(confMap, CONF_THREAD_MAX, THREAD_MIN);
	}

	public boolean setThreadMax(int threadMax){
		Assert.isTrue(threadMax >= THREAD_MIN, buildMsg(CONF_THREAD_MAX, " cannot be less than ", THREAD_MIN)); 
		if(threadMax == getThreadMax()){
			return false;
		}

		confMap.put(CONF_THREAD_MAX, threadMax);
		int threadCore = getThreadCore();
		if(threadCore > threadMax){
			threadCore = threadMax;
			confMap.put(CONF_THREAD_CORE, threadCore);
		}

		if(pool != null && pool.getMaximumPoolSize() != threadMax){
			pool.setCorePoolSize(threadCore);
			pool.setMaximumPoolSize(threadMax);
		}
		return true;
	}

	public int getThreadAliveTime(){
		return MapUtils.getIntValue(confMap, CONF_THREAD_ALIVETIME, THREAD_ALIVETIME_DEFAULT);
	}

	public boolean setThreadAliveTime(int aliveTime){
		Assert.isTrue(aliveTime >= THREAD_ALIVETIME_MIN, buildMsg(CONF_THREAD_ALIVETIME, " cannot be less than ", THREAD_ALIVETIME_MIN)); 
		if(aliveTime == getThreadAliveTime()){
			return false;
		}
		confMap.put(CONF_THREAD_ALIVETIME, aliveTime);
		if(pool != null && pool.getKeepAliveTime(TimeUnit.SECONDS) != aliveTime){ 
			pool.setKeepAliveTime(aliveTime, TimeUnit.SECONDS);
		}
		return true;
	}

	public int getTaskOverTime(){
		return MapUtils.getIntValue(confMap, CONF_TASK_OVERTIME, TASK_OVERTIME_DEFAULT);
	}

	public boolean setTaskOverTime(int overTime){
		Assert.isTrue(overTime >= TASK_OVERTIME_DEFAULT, buildMsg(CONF_TASK_OVERTIME, " cannot be less than ", TASK_OVERTIME_DEFAULT)); 
		if(overTime == getTaskOverTime()){
			return false;
		}
		confMap.put(CONF_TASK_OVERTIME, overTime);
		return true;
	}

	public boolean getExecOnLoad(){
		return MapUtils.getBoolean(confMap, CONF_EXECONLOAN, EXECONLOAN_DEFAULT);
	}

	public boolean setExecOnLoad(boolean execOnLoad) {
		if(execOnLoad == getExecOnLoad()){
			return false;
		}
		confMap.put(CONF_EXECONLOAN, execOnLoad);
		return true;
	}

	public int getQueueSize(){
		return MapUtils.getIntValue(confMap, CONF_QUEUESIZE, QUEUE_SIZE_DEFAULT);
	}

	boolean setQueueSize(int queueSize){
		Assert.isTrue(queueSize >= QUEUE_SIZE_MIN, buildMsg(CONF_QUEUESIZE, " cannot be less than ", QUEUE_SIZE_MIN)); 
		if(queueSize == getQueueSize()){
			return false;
		}
		confMap.put(CONF_QUEUESIZE, queueSize);
		return true;
	}

	@Override
	public String toString() {
		return confMap.toString();
	}
}
