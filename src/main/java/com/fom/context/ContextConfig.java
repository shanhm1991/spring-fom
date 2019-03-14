package com.fom.context;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.quartz.CronExpression;

import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 *
 */
public final class ContextConfig implements Serializable {

	private static final long serialVersionUID = -580843719008019078L;

	/**
	 * 定时表达式
	 */
	public static final String CRON = "cron";

	/**
	 * 备注
	 */
	public static final String REMARK = "remark";

	/**
	 * 线程池任务队列长度
	 */
	public static final String QUEUESIZE = "queueSize";

	/**
	 * 线程池核心线程数
	 */
	public static final String THREADCORE = "threadCore";

	/**
	 * 线程池最大线程数
	 */
	public static final String THREADMAX = "threadMax";

	/**
	 * 线程池任务线程最长空闲时间
	 */
	public static final String ALIVETIME = "threadAliveTime";

	/**
	 * 线程池任务线程执行超时时间
	 */
	public static final String OVERTIME = "threadOverTime";

	/**
	 * 线程池任务线程如果超时是否中断
	 */
	public static final String CANCELLABLE = "cancellable";

	/** 
	 * 如果没有配置定时周期，是否在执行完批量任务后自行结束
	 */
	public static final String STOPWITHNOCRON = "stopWithNoCron";


	public static boolean validKey(String key){
		return !THREADCORE.equals(key) && !THREADMAX.equals(key) && !ALIVETIME.equals(key) && !STOPWITHNOCRON.equals(key)
				&& !OVERTIME.equals(key) && !QUEUESIZE.equals(key) && !CANCELLABLE.equals(key) && !CRON.equals(key) ;
	}

	transient TimedExecutorPool pool;

	volatile CronExpression cronExpression;

	//valueMap只允许put动作，在put时先判断key是否存在，再判断value是否相等，可以很好的避免线程安全问题
	Map<String, String> valueMap = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	void init(Element element, Map<String, String> cMap, FomContext fc){
		if(element != null){
			List<Element> list = element.elements();
			for(Element e : list){
				String value = e.getTextTrim();
				if(StringUtils.isBlank(value)){
					valueMap.put(e.getName(), e.asXML());
				}else{
					valueMap.put(e.getName(), e.getTextTrim());
				}
			}
			setThreadCore(XmlUtil.getInt(element, ContextConfig.THREADCORE, 4, 1, 10));
			setThreadMax(XmlUtil.getInt(element, ContextConfig.THREADMAX, 20, 10, 100));
			setAliveTime(XmlUtil.getInt(element, ContextConfig.ALIVETIME, 30, 5, 300));
			setOverTime(XmlUtil.getInt(element, ContextConfig.OVERTIME, 3600, 60, 86400));
			setQueueSize(XmlUtil.getInt(element, ContextConfig.QUEUESIZE, 200, 1, 10000000));
			setCancellable(XmlUtil.getBoolean(element, ContextConfig.CANCELLABLE, false));
			setCron(XmlUtil.getString(element, ContextConfig.CRON, ""));
			setRemark(XmlUtil.getString(element, ContextConfig.REMARK, ""));
			setStopWithNoCron(XmlUtil.getBoolean(element, ContextConfig.STOPWITHNOCRON, false));
		}else if(cMap != null){
			valueMap.putAll(cMap);
			setThreadCore(getInt(cMap, ContextConfig.THREADCORE));
			setThreadMax(getInt(cMap, ContextConfig.THREADMAX));
			setAliveTime(getInt(cMap, ContextConfig.ALIVETIME));
			setOverTime(getInt(cMap, ContextConfig.OVERTIME));
			setQueueSize(getInt(cMap, ContextConfig.QUEUESIZE));
			setCancellable(getBoolean(cMap, ContextConfig.CANCELLABLE));
			setCron(cMap.get(ContextConfig.CRON)); 
			setRemark(cMap.get(ContextConfig.REMARK));
			setStopWithNoCron(getBoolean(cMap,ContextConfig.STOPWITHNOCRON));
		}else if(fc != null){
			setThreadCore(fc.threadCore());
			setThreadMax(fc.threadMax());
			setAliveTime(fc.threadAliveTime());
			setOverTime(fc.threadOverTime());
			setQueueSize(fc.queueSize());
			setCancellable(fc.cancellable());
			setCron(fc.cron());
			setRemark(fc.remark());
			setStopWithNoCron(fc.stopWithNoCron());
		}else{
			setThreadCore(4);
			setThreadMax(20);
			setAliveTime(30);
			setOverTime(3600);
			setQueueSize(200);
			setCancellable(false);
			setStopWithNoCron(false);
		}

		initPool();
	}

	void initPool(){
		int core = Integer.parseInt(valueMap.get(ContextConfig.THREADCORE)); 
		int max = Integer.parseInt(valueMap.get(ContextConfig.THREADMAX));
		int aliveTime = Integer.parseInt(valueMap.get(ContextConfig.ALIVETIME));   
		int queueSize = Integer.parseInt(valueMap.get(ContextConfig.QUEUESIZE));  
		pool = new TimedExecutorPool(core,max,aliveTime,new LinkedBlockingQueue<Runnable>(queueSize));
		pool.allowCoreThreadTimeOut(true);
	}

	int getInt(Map<String, String> map, String key){
		try{
			return Integer.parseInt(map.get(key)); 
		}catch(Exception e){
			return -1;
		}
	}

	boolean getBoolean(Map<String, String> map, String key){
		try{
			return Boolean.parseBoolean(map.get(key)); 
		}catch(Exception e){
			return false;
		}
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
		if(queueSize < 1 || queueSize > 10000000){
			queueSize = 200;
		}
		valueMap.put(ContextConfig.QUEUESIZE, String.valueOf(queueSize));
		return queueSize;
	}

	/**
	 * 保存配置key-value
	 * @param key key
	 * @param value value
	 */
	public void put(String key, String value) {
		if(!ContextConfig.validKey(key)){
			throw new IllegalArgumentException("not support key:" + key);
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
		try{
			return Integer.parseInt(String.valueOf(valueMap.get(key)));
		}catch(Exception e){
			return defaultValue;
		}
	}

	/**
	 * 获取配置key的long值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public long getLong(String key, long defaultValue){
		try{
			return Long.parseLong(String.valueOf(valueMap.get(key)));
		}catch(Exception e){
			return defaultValue;
		}
	}

	/**
	 * 获取配置key的boolean值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public boolean getBoolean(String key, boolean defaultValue){
		try{
			return Boolean.parseBoolean(String.valueOf(valueMap.get(key)));
		}catch(Exception e){
			return defaultValue;
		}
	}

	/**
	 * 获取配置key的string值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public String getString(String key, String defaultValue){
		String value = String.valueOf(valueMap.get(key));
		if(StringUtils.isBlank(value)){
			return defaultValue;
		}
		return value;
	}

	/**
	 * 获取配置的备注信息
	 * @return remark
	 */
	public String getRemark(){
		return valueMap.get(ContextConfig.REMARK);
	}

	/**
	 * 设置remark备注信息
	 * @param remark remark
	 */
	public void setRemark(String remark){
		valueMap.put(ContextConfig.REMARK, remark);
	}

	/**
	 * 获取本地线程池的核心线程数
	 * @return threadCore
	 */
	public int getThreadCore(){
		return Integer.parseInt(valueMap.get(ContextConfig.THREADCORE));
	}

	/**
	 * 配置本地线程池的核心线程数，将在下一次运行生效
	 * @param threadCore threadCore
	 * @return threadCore
	 */
	public int setThreadCore(int threadCore){
		if(threadCore < 1 || threadCore > 10){
			threadCore = 4;
		}
		valueMap.put(ContextConfig.THREADCORE, String.valueOf(threadCore));
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
		return Integer.parseInt(valueMap.get(ContextConfig.THREADMAX));
	}

	/**
	 * 配置本地线程池的最大线程数，将在下一次运行生效
	 * @param threadMax threadMax
	 * @return threadMax
	 */
	public int setThreadMax(int threadMax){
		if(threadMax < 10 || threadMax > 100){
			threadMax = 20;
		}
		valueMap.put(ContextConfig.THREADMAX, String.valueOf(threadMax));
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
		return Integer.parseInt(valueMap.get(ContextConfig.ALIVETIME));
	}

	/**
	 * 配置本地线程池的线程存活时间，将在下一次运行生效
	 * @param aliveTime aliveTime
	 * @return aliveTime
	 */
	public int setAliveTime(int aliveTime){
		if(aliveTime < 3 || aliveTime > 600){
			aliveTime = 30;
		}
		valueMap.put(ContextConfig.ALIVETIME, String.valueOf(aliveTime));
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
		return Integer.parseInt(valueMap.get(ContextConfig.OVERTIME));
	}

	/**
	 * 配置任务线程的超时时间，将在下一次运行生效
	 * @param overTime overTime
	 * @return overTime
	 */
	public int setOverTime(int overTime){
		if(overTime < 60 || overTime > 86400){
			overTime = 3600;
		}
		valueMap.put(ContextConfig.OVERTIME, String.valueOf(overTime));
		return overTime;
	}

	/**
	 * 获取cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @return cancellable
	 */
	public boolean getCancellable(){
		return Boolean.parseBoolean(valueMap.get(ContextConfig.CANCELLABLE));
	}

	/**
	 * 配置cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @param cancellable cancellable
	 * @return cancellable
	 */
	public boolean setCancellable(boolean cancellable){
		valueMap.put(ContextConfig.CANCELLABLE, String.valueOf(cancellable));
		return cancellable;
	}

	/**
	 * 获取context定时表达式
	 * @return cron
	 */
	public String getCron(){
		return valueMap.get(ContextConfig.CRON);
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
		CronExpression c = null;
		try {
			c = new CronExpression(cron);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		if(cronExpression == null
				|| !(cron.equals(valueMap.get(ContextConfig.CRON)  ))){
			valueMap.put(ContextConfig.CRON, cron);
			cronExpression = c;
		}
	}
	
	/**
	 * 没有设置定时周期，是否在执行完批任务后就关闭
	 * @param stopWithNoCron stopWithNoCron
	 */
	public void setStopWithNoCron(boolean stopWithNoCron) {
		valueMap.put(ContextConfig.STOPWITHNOCRON, String.valueOf(stopWithNoCron));
	}
	
	/**
	 * 获取配置，是否在执行完批任务后就关闭
	 * @return stopWithNoCron
	 */
	public boolean getStopWithNoCron() {
		return getBoolean(valueMap, ContextConfig.STOPWITHNOCRON);
	}
}
