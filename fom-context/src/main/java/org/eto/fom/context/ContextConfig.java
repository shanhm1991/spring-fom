package org.eto.fom.context;

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
import org.eto.fom.util.map.MapUtils;
import org.eto.fom.util.xml.XmlUtil;
import org.quartz.CronExpression;

/**
 * context实例的配置管理
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

	/** 
	 * 启动时是否立即执行定时批量任务
	 */
	public static final String EXECONLOAN = "execOnLoad";

	public static boolean validKey(String key){
		return !THREADCORE.equals(key) && !THREADMAX.equals(key) && !ALIVETIME.equals(key) && !STOPWITHNOCRON.equals(key)
				&& !OVERTIME.equals(key) && !QUEUESIZE.equals(key) && !CANCELLABLE.equals(key) && !CRON.equals(key) 
				&& !EXECONLOAN.equals(key);
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
			setThreadCore(XmlUtil.getInt(element, THREADCORE, THREADCORE_DEFAULT, THREADCORE_MIN, THREADCORE_MAX));
			setThreadMax(XmlUtil.getInt(element, THREADMAX, THREADMAX_DEFAULT, THREADMAX_MIN, THREADMAX_MAX));
			setAliveTime(XmlUtil.getInt(element, ALIVETIME, ALIVETIME_DEFAULT, ALIVETIME_MIN, ALIVETIME_MAX));
			setOverTime(XmlUtil.getInt(element, OVERTIME, OVERTIME_DEFAULT, OVERTIME_MIN, OVERTIME_MAX));
			setQueueSize(XmlUtil.getInt(element, QUEUESIZE, QUEUESIZE_DEFAULT, QUEUESIZE_MIN, QUEUESIZE_MAX));
			setCancellable(XmlUtil.getBoolean(element, CANCELLABLE, false));
			setCron(XmlUtil.getString(element, CRON, ""));
			setRemark(XmlUtil.getString(element, REMARK, ""));
			setStopWithNoCron(XmlUtil.getBoolean(element, STOPWITHNOCRON, false));
			setExecOnLoad(XmlUtil.getBoolean(element, EXECONLOAN, true));
		}else if(cMap != null){
			valueMap.putAll(cMap);
			setThreadCore(MapUtils.getInt(THREADCORE, cMap, THREADCORE_DEFAULT));
			setThreadMax(MapUtils.getInt(THREADMAX, cMap, THREADMAX_DEFAULT));
			setAliveTime(MapUtils.getInt(ALIVETIME, cMap, ALIVETIME_DEFAULT));
			setOverTime(MapUtils.getInt(OVERTIME, cMap, OVERTIME_DEFAULT));
			setQueueSize(MapUtils.getInt(QUEUESIZE, cMap, QUEUESIZE_DEFAULT));
			setCron(cMap.get(CRON)); 
			setRemark(cMap.get(REMARK));
			setCancellable(MapUtils.getBoolean(CANCELLABLE, cMap, false));
			setStopWithNoCron(MapUtils.getBoolean(STOPWITHNOCRON, cMap, false));
			setExecOnLoad(MapUtils.getBoolean(EXECONLOAN, cMap, true));
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
			setExecOnLoad(fc.execOnLoad());
		}else{
			setThreadCore(THREADCORE_DEFAULT);
			setThreadMax(THREADMAX_DEFAULT);
			setAliveTime(ALIVETIME_DEFAULT);
			setOverTime(OVERTIME_DEFAULT);
			setQueueSize(QUEUESIZE_DEFAULT);
			setCancellable(false);
			setStopWithNoCron(false);
			setExecOnLoad(true);
		}

		initPool();
	}

	void initPool(){
		int core = Integer.parseInt(valueMap.get(THREADCORE)); 
		int max = Integer.parseInt(valueMap.get(THREADMAX));
		int aliveTime = Integer.parseInt(valueMap.get(ALIVETIME));   
		int queueSize = Integer.parseInt(valueMap.get(QUEUESIZE));  
		pool = new TimedExecutorPool(core,max,aliveTime,new LinkedBlockingQueue<Runnable>(queueSize));
		pool.allowCoreThreadTimeOut(true);
	}

//	boolean getBoolean(Map<String, String> map, String key){
//		try{
//			return Boolean.parseBoolean(map.get(key)); 
//		}catch(Exception e){
//			return false;
//		}
//	}

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
		valueMap.put(QUEUESIZE, String.valueOf(queueSize));
		return queueSize;
	}

	/**
	 * 保存配置key-value
	 * @param key key
	 * @param value value
	 */
	public void put(String key, String value) {
		if(!validKey(key)){
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
		return valueMap.get(REMARK);
	}

	/**
	 * 设置remark备注信息
	 * @param remark remark
	 */
	public void setRemark(String remark){
		valueMap.put(REMARK, remark);
	}

	/**
	 * 获取本地线程池的核心线程数
	 * @return threadCore
	 */
	public int getThreadCore(){
		return Integer.parseInt(valueMap.get(THREADCORE));
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
		valueMap.put(THREADCORE, String.valueOf(threadCore));
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
		return Integer.parseInt(valueMap.get(THREADMAX));
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
		valueMap.put(THREADMAX, String.valueOf(threadMax));
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
		return Integer.parseInt(valueMap.get(ALIVETIME));
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
		valueMap.put(ALIVETIME, String.valueOf(aliveTime));
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
		return Integer.parseInt(valueMap.get(OVERTIME));
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
		valueMap.put(OVERTIME, String.valueOf(overTime));
		return overTime;
	}

	/**
	 * 获取cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @return cancellable
	 */
	public boolean getCancellable(){
		return Boolean.parseBoolean(valueMap.get(CANCELLABLE));
	}

	/**
	 * 配置cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @param cancellable cancellable
	 * @return cancellable
	 */
	public boolean setCancellable(boolean cancellable){
		valueMap.put(CANCELLABLE, String.valueOf(cancellable));
		return cancellable;
	}

	/**
	 * 获取context定时表达式
	 * @return cron
	 */
	public String getCron(){
		return valueMap.get(CRON);
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
				|| !(cron.equals(valueMap.get(CRON)  ))){
			valueMap.put(CRON, cron);
			cronExpression = c;
		}
	}

	/**
	 * 没有设置定时周期，是否在执行完批任务后就关闭
	 * @param stopWithNoCron stopWithNoCron
	 */
	public void setStopWithNoCron(boolean stopWithNoCron) {
		valueMap.put(STOPWITHNOCRON, String.valueOf(stopWithNoCron));
	}

	/**
	 * 获取配置，是否在执行完批任务后就关闭
	 * @return stopWithNoCron
	 */
	public boolean getStopWithNoCron() {
		return MapUtils.getBoolean(STOPWITHNOCRON, valueMap, false);
	}

	/**
	 * 设置启动时是否立即执行定时批量任务
	 * @param execOnLoad execOnLoad
	 */
	public void setExecOnLoad(boolean execOnLoad) {
		valueMap.put(EXECONLOAN, String.valueOf(execOnLoad));
	}

	/**
	 * 获取配置，启动时是否立即执行定时批量任务
	 * @return execOnLoad
	 */
	public boolean getExecOnLoad(){
		return MapUtils.getBoolean(EXECONLOAN, valueMap, true);
	}

	private static final int THREADCORE_DEFAULT = 4;

	private static final int THREADCORE_MIN = 1;

	private static final int THREADCORE_MAX = 10;

	private static final int THREADMAX_DEFAULT = 20;

	private static final int THREADMAX_MIN = 10;

	private static final int THREADMAX_MAX = 100;

	private static final int ALIVETIME_DEFAULT = 30;

	private static final int ALIVETIME_MIN = 5;

	private static final int ALIVETIME_MAX = 300;

	private static final int OVERTIME_DEFAULT = 36000;

	private static final int OVERTIME_MIN = 1;

	private static final int OVERTIME_MAX = 86400;

	private static final int QUEUESIZE_DEFAULT = 200;

	private static final int QUEUESIZE_MIN = 1;

	private static final int QUEUESIZE_MAX = 10000000;

}
