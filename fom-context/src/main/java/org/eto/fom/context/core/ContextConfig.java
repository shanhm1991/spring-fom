package org.eto.fom.context.core;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eto.fom.context.SpringContext;
import org.quartz.CronExpression;

import com.google.gson.annotations.Expose;

/**
 * context实例的配置管理
 * 
 * @author shanhm
 *
 */
public final class ContextConfig {

	/**
	 * 定时表达式
	 */
	public static final String CONF_CRON = "cron";
	
	/**
	 * 定时周期
	 */
	public static final String CONF_FIXEDRATE = "fixedRate";
	
	/**
	 * 定时周期
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
	public static final String CONF_THREADCORE = "threadCore";

	/**
	 * 线程池最大线程数
	 */
	public static final String CONF_THREADMAX = "threadMax";

	/**
	 * 线程池任务线程最长空闲时间
	 */
	public static final String CONF_ALIVETIME = "threadAliveTime";

	/**
	 * 线程池任务线程执行超时时间
	 */
	public static final String CONF_OVERTIME = "threadOverTime";

	/**
	 * 线程池任务线程如果超时是否中断
	 */
	public static final String CONF_CANCELLABLE = "cancellable";

	/** 
	 * 如果没有配置定时周期，是否在执行完批量任务后自行结束
	 */
	public static final String CONF_STOPWITHNOCRON = "stopWithNoCron";

	/** 
	 * 启动时是否立即执行定时批量任务
	 */
	public static final String CONF_EXECONLOAN = "execOnLoad";
	
	public static final int THREADCORE_DEFAULT = 4;

	public static final int THREADCORE_MIN = 1;

	public static final int THREADCORE_MAX = 100;

	public static final int THREADMAX_DEFAULT = 200;

	public static final int THREADMAX_MIN = 100;

	public static final int THREADMAX_MAX = 1000;

	public static final int ALIVETIME_DEFAULT = 30;

	public static final int ALIVETIME_MIN = 5;

	public static final int ALIVETIME_MAX = 300;

	public static final int OVERTIME_DEFAULT = 3600;

	public static final int OVERTIME_MIN = 1;

	public static final int OVERTIME_MAX = 86400;

	public static final int QUEUESIZE_DEFAULT = 2000;

	public static final int QUEUESIZE_MIN = 1;

	public static final int QUEUESIZE_MAX = Integer.MAX_VALUE;

	//已加载的配置
	static final Map<String, ConcurrentHashMap<String,String>> loadedConfig = new HashMap<>();

	@Expose
	TimedExecutorPool pool;

	@Expose
	volatile CronExpression cronExpression;

	final ConcurrentHashMap<String, String> valueMap; 

	public static boolean validKey(String key){
		return !CONF_THREADCORE.equals(key) && !CONF_THREADMAX.equals(key) && !CONF_QUEUESIZE.equals(key) 
				&& !CONF_ALIVETIME.equals(key) && !CONF_OVERTIME.equals(key) && !CONF_CANCELLABLE.equals(key)
				&& !CONF_CRON.equals(key) && !CONF_FIXEDRATE.equals(key) && !CONF_FIXEDDELAY.equals(key) 
				&& !CONF_STOPWITHNOCRON.equals(key) && !CONF_EXECONLOAN.equals(key);
	}

	ContextConfig(String name){
		if(loadedConfig.containsKey(name)){
			valueMap = loadedConfig.remove(name);
		}else{
			valueMap = new ConcurrentHashMap<>();
		}

		setThreadCore(MapUtils.getInt(CONF_THREADCORE, valueMap, THREADCORE_DEFAULT));
		setThreadMax(MapUtils.getInt(CONF_THREADMAX, valueMap, THREADMAX_DEFAULT));
		setAliveTime(MapUtils.getInt(CONF_ALIVETIME, valueMap, ALIVETIME_DEFAULT));
		setOverTime(MapUtils.getInt(CONF_OVERTIME, valueMap, OVERTIME_DEFAULT));
		setQueueSize(MapUtils.getInt(CONF_QUEUESIZE, valueMap, QUEUESIZE_DEFAULT));
		setCron(valueMap.get(CONF_CRON));
		setFixedRate(MapUtils.getInt(CONF_FIXEDRATE, valueMap, 0)); 
		setFixedDelay(MapUtils.getInt(CONF_FIXEDDELAY, valueMap, 0)); 
		setRemark(valueMap.get(CONF_REMARK));
		setCancellable(MapUtils.getBoolean(CONF_CANCELLABLE, valueMap, false));
		setStopWithNoCron(MapUtils.getBoolean(CONF_STOPWITHNOCRON, valueMap, false));
		setExecOnLoad(MapUtils.getBoolean(CONF_EXECONLOAN, valueMap, false));
		
		init();
	}
	
	void init(){
		int core = getInt(CONF_THREADCORE, THREADCORE_DEFAULT); 
		int max = getInt(CONF_THREADMAX, THREADMAX_DEFAULT);
		int aliveTime = getInt(CONF_ALIVETIME, ALIVETIME_DEFAULT);   
		int queueSize = getInt(CONF_QUEUESIZE, QUEUESIZE_DEFAULT);  
		pool = new TimedExecutorPool(core,max,aliveTime,new LinkedBlockingQueue<Runnable>(queueSize));
		pool.allowCoreThreadTimeOut(true);
	}

	long getActives(){
		if(pool == null){
			return 0;
		}
		return pool.getActiveCount();
	}

	int getWaitings(){
		if(pool == null){
			return 0;
		}
		return pool.getQueue().size();
	}

	@SuppressWarnings("rawtypes")
	Map<String, Object> getWaitingDetail(){
		Map<String, Object> map = new HashMap<>();
		if(pool == null){
			return map;
		}

		Object[] array = pool.getQueue().toArray();
		if(ArrayUtils.isEmpty(array)){
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

	long getCreated(){
		if(pool == null){
			return 0;
		}
		return pool.getTaskCount();
	}

	long getCompleted(){
		if(pool == null){
			return 0;
		}
		return pool.getCompletedTaskCount();
	}

	Map<Task<?>, Thread> getActiveThreads() {
		if(pool == null){
			return new HashMap<Task<?>, Thread>();
		}
		return pool.getActiveThreads();
	}

	int setQueueSize(int queueSize){
		if(queueSize < QUEUESIZE_MIN || queueSize > QUEUESIZE_MAX){
			queueSize = QUEUESIZE_DEFAULT;
		}
		valueMap.put(CONF_QUEUESIZE, String.valueOf(queueSize));
		return queueSize;
	}

	/**
	 * 保存配置key-value
	 * @param key key
	 * @param value value
	 */
	public void put(String key, String value) {
		if(!validKey(key)){
			throw new UnsupportedOperationException("not support key:" + key);
		}
		if(value == null){
			return;
		}
		valueMap.put(key, value);
	}

	/**
	 * 获取配置
	 * @param key key
	 * @return value
	 */
	public String get(String key){
		return valueMap.get(key);
	}

	/**
	 * 获取配置key的int值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public int getInt(String key, int defaultValue){
		return MapUtils.getInt(key, valueMap, defaultValue);
	}

	/**
	 * 获取配置key的long值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public long getLong(String key, long defaultValue){
		return MapUtils.getLong(key, valueMap, defaultValue);
	}

	/**
	 * 获取配置key的boolean值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public boolean getBoolean(String key, boolean defaultValue){
		return MapUtils.getBoolean(key, valueMap, defaultValue);
	}

	/**
	 * 获取配置key的string值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public String getString(String key, String defaultValue){
		return MapUtils.getString(key, valueMap, defaultValue);
	}

	/**
	 * 获取配置的备注信息
	 * @return remark
	 */
	public String getRemark(){
		return valueMap.get(CONF_REMARK);
	}

	/**
	 * 设置remark备注信息
	 * @param remark remark
	 */
	public void setRemark(String remark){
		if(remark == null){
			return;
		}
		valueMap.put(CONF_REMARK, remark);
	}

	/**
	 * 获取本地线程池的核心线程数
	 * @return threadCore
	 */
	public int getThreadCore(){
		return Integer.parseInt(valueMap.get(CONF_THREADCORE));
	}

	/**
	 * 配置本地线程池的核心线程数，将在下一次运行生效
	 * @param threadCore threadCore
	 * @return threadCore
	 */
	public int setThreadCore(int threadCore){
		if(threadCore < THREADCORE_MIN || threadCore > THREADCORE_MAX){
			threadCore = THREADCORE_DEFAULT;
		}
		valueMap.put(CONF_THREADCORE, String.valueOf(threadCore));
		if(pool != null && pool.getCorePoolSize() != threadCore){
			pool.setCorePoolSize(threadCore);
		}
		return threadCore;
	}

	/**
	 * 获取本地线程池的最大线程数
	 * @return threadMax
	 */
	public int getThreadMax(){
		return Integer.parseInt(valueMap.get(CONF_THREADMAX));
	}

	/**
	 * 配置本地线程池的最大线程数，将在下一次运行生效
	 * @param threadMax threadMax
	 * @return threadMax
	 */
	public int setThreadMax(int threadMax){
		if(threadMax < THREADMAX_MIN || threadMax > THREADMAX_MAX){
			threadMax = THREADMAX_DEFAULT;
		}
		valueMap.put(CONF_THREADMAX, String.valueOf(threadMax));
		if(pool != null && pool.getMaximumPoolSize() != threadMax){
			pool.setMaximumPoolSize(threadMax);
		}
		return threadMax;
	}

	/**
	 * 获取本地线程池的线程存活时间
	 * @return aliveTime
	 */
	public int getAliveTime(){
		return Integer.parseInt(valueMap.get(CONF_ALIVETIME));
	}

	/**
	 * 配置本地线程池的线程存活时间，将在下一次运行生效
	 * @param aliveTime aliveTime
	 * @return aliveTime
	 */
	public int setAliveTime(int aliveTime){
		if(aliveTime < ALIVETIME_MIN || aliveTime > ALIVETIME_MAX){
			aliveTime = ALIVETIME_DEFAULT;
		}
		valueMap.put(CONF_ALIVETIME, String.valueOf(aliveTime));
		if(pool != null && pool.getKeepAliveTime(TimeUnit.SECONDS) != aliveTime){ 
			pool.setKeepAliveTime(aliveTime, TimeUnit.SECONDS);
		}
		return aliveTime;
	}

	/**
	 * 获取任务线程的超时时间
	 * @return overTime
	 */
	public int getOverTime(){
		return Integer.parseInt(valueMap.get(CONF_OVERTIME));
	}

	/**
	 * 配置任务线程的超时时间，将在下一次运行生效
	 * @param overTime overTime
	 * @return overTime
	 */
	public int setOverTime(int overTime){
		if(overTime < OVERTIME_MIN || overTime > OVERTIME_MAX){
			overTime = OVERTIME_DEFAULT;
		}
		valueMap.put(CONF_OVERTIME, String.valueOf(overTime));
		return overTime;
	}

	/**
	 * 获取cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @return cancellable
	 */
	public boolean getCancellable(){
		return Boolean.parseBoolean(valueMap.get(CONF_CANCELLABLE));
	}

	/**
	 * 配置cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @param cancellable cancellable
	 * @return cancellable
	 */
	public boolean setCancellable(boolean cancellable){
		valueMap.put(CONF_CANCELLABLE, String.valueOf(cancellable));
		return cancellable;
	}

	/**
	 * 获取context定时表达式
	 * @return cron
	 */
	public String getCron(){
		return valueMap.get(CONF_CRON);
	}

	/**
	 * 配置context定时表达式
	 * <br>如果之前存在，将在下一次运行生效，否则需要重新激活（startup）才能生效
	 * @param cron cron
	 */
	public void setCron(String cron){
		if(StringUtils.isBlank(cron)){
			return;
		}

		//可能来自注解
		if(cron.indexOf("${") != -1){ 
			cron = SpringContext.getPropertiesValue(cron);
		}

		CronExpression c = null;
		try {
			c = new CronExpression(cron);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		
		if(cronExpression == null
				|| !(cronExpression.getCronExpression().equals(cron.toUpperCase(Locale.US)))){
			valueMap.put(CONF_CRON, cron);
			cronExpression = c;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public int getFixedRate(){
		return Integer.parseInt(valueMap.get(CONF_FIXEDRATE)); 
	}
	
	/**
	 * 
	 * @param fixedRate
	 */
	public void setFixedRate(int fixedRate){
		fixedRate = fixedRate > 0 ? fixedRate : 0;
		valueMap.put(CONF_FIXEDRATE, String.valueOf(fixedRate));
	}
	
	/**
	 * 
	 * @return
	 */
	public int getFixedDelay(){
		return Integer.parseInt(valueMap.get(CONF_FIXEDDELAY));
	}
	
	/**
	 * 
	 * @param fixedDelay
	 */
	public void setFixedDelay(int fixedDelay){
		fixedDelay = fixedDelay > 0 ? fixedDelay : 0;
		valueMap.put(CONF_FIXEDDELAY, String.valueOf(fixedDelay));
	}

	/**
	 * 没有设置定时周期，是否在执行完批任务后就关闭
	 * @param stopWithNoCron stopWithNoCron
	 */
	public void setStopWithNoCron(boolean stopWithNoCron) {
		valueMap.put(CONF_STOPWITHNOCRON, String.valueOf(stopWithNoCron));
	}

	/**
	 * 获取配置，是否在执行完批任务后就关闭
	 * @return stopWithNoCron
	 */
	public boolean getStopWithNoCron() {
		return MapUtils.getBoolean(CONF_STOPWITHNOCRON, valueMap, false);
	}

	/**
	 * 设置启动时是否立即执行定时批量任务
	 * @param execOnLoad execOnLoad
	 */
	public void setExecOnLoad(boolean execOnLoad) {
		valueMap.put(CONF_EXECONLOAN, String.valueOf(execOnLoad));
	}

	/**
	 * 获取配置，启动时是否立即执行定时批量任务
	 * @return execOnLoad
	 */
	public boolean getExecOnLoad(){
		return MapUtils.getBoolean(CONF_EXECONLOAN, valueMap, false);
	}

	@Override
	public String toString() {
		return valueMap.toString();
	}

}
