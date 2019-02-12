package com.fom.context;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hdfs.server.namenode.UnsupportedActionException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.quartz.CronExpression;

import com.fom.log.LoggerFactory;
import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 *
 */
public abstract class Context implements Serializable {

	private static final long serialVersionUID = 9154119563307298882L;

	//所有的Context共用，防止两个Context创建针对同一个文件的任务
	private static final Map<String,TimedFuture<Result>> FUTUREMAP = new ConcurrentHashMap<>(500);

	static final String CRON = "cron";

	static final String REMARK = "remark";

	static final String QUEUESIZE = "queueSize";

	static final String THREADCORE = "threadCore";

	static final String THREADMAX = "threadMax";

	static final String ALIVETIME = "threadAliveTime";

	static final String OVERTIME = "threadOverTime";

	static final String CANCELLABLE = "cancellable";

	protected final String name;

	protected transient Logger log;

	volatile long createTime;

	volatile long startTime;

	private transient TimedExecutorPool pool;

	public Context(){
		Class<?> clazz = this.getClass();
		FomContext fc = clazz.getAnnotation(FomContext.class);
		if(fc == null){
			this.name = clazz.getSimpleName();
		}else{
			if(StringUtils.isBlank(fc.name())){
				this.name = clazz.getSimpleName();
			}else{
				this.name = fc.name();
			}
		}
		initValue(name, fc); 
	}

	public Context(String name){
		if(StringUtils.isBlank(name)){
			throw new IllegalArgumentException("param name cann't be empty.");
		}
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
		Class<?> clazz = this.getClass();
		FomContext fc = clazz.getAnnotation(FomContext.class);
		initValue(name, fc);
	}

	//valueMap只允许put动作，在put时先判断key是否存在，再判断value是否相等，可以很好的避免线程安全问题
	Map<String, String> valueMap = new ConcurrentHashMap<>();

	private volatile CronExpression cronExpression;

	/**
	 * xml > 注解  > 默认
	 * @param name
	 * @param fc
	 */
	@SuppressWarnings("unchecked")
	private void initValue(String name,FomContext fc){
		Element element = ContextManager.elementMap.get(name); 
		Map<String, String> cMap = ContextManager.createMap.get(name);
		if(element != null){
			List<Element> list = element.elements();
			for(Element e : list){
				valueMap.put(e.getName(), e.getTextTrim());
			}
			setThreadCore(XmlUtil.getInt(element, THREADCORE, 4, 1, 10));
			setThreadMax(XmlUtil.getInt(element, THREADMAX, 20, 10, 100));
			setAliveTime(XmlUtil.getInt(element, ALIVETIME, 30, 5, 300));
			setOverTime(XmlUtil.getInt(element, OVERTIME, 3600, 60, 86400));
			setQueueSize(XmlUtil.getInt(element, QUEUESIZE, 200, 1, 10000000));
			setCancellable(XmlUtil.getBoolean(element, CANCELLABLE, false));
			setCron(XmlUtil.getString(element, CRON, ""));
			setRemark(XmlUtil.getString(element, REMARK, ""));
		}else if(cMap != null){
			valueMap.putAll(cMap);
			setThreadCore(getInt(cMap, THREADCORE));
			setThreadMax(getInt(cMap, THREADMAX));
			setAliveTime(getInt(cMap, ALIVETIME));
			setOverTime(getInt(cMap, OVERTIME));
			setQueueSize(getInt(cMap, QUEUESIZE));
			setCancellable(getBoolean(cMap, CANCELLABLE));
			setCron(cMap.get(CRON)); 
			setRemark(cMap.get(REMARK));
		}else if(fc != null){
			setThreadCore(fc.threadCore());
			setThreadMax(fc.threadMax());
			setAliveTime(fc.threadAliveTime());
			setOverTime(fc.threadOverTime());
			setQueueSize(fc.queueSize());
			setCancellable(fc.cancellable());
			setCron(fc.cron());
			setRemark(fc.remark());
		}else{
			setThreadCore(4);
			setThreadMax(20);
			setAliveTime(30);
			setOverTime(3600);
			setQueueSize(200);
			setCancellable(false);
		}
		initPool();
		createTime = System.currentTimeMillis();
		ContextManager.register(this); 
	}

	private int getInt(Map<String, String> map, String key){
		try{
			return Integer.parseInt(map.get(key)); 
		}catch(Exception e){
			return -1;
		}
	}

	private boolean getBoolean(Map<String, String> map, String key){
		try{
			return Boolean.parseBoolean(map.get(key)); 
		}catch(Exception e){
			return false;
		}
	}

	//反序列化时额外的初始化工作
	void initPool(){
		int core = Integer.parseInt(valueMap.get(THREADCORE)); 
		int max = Integer.parseInt(valueMap.get(THREADMAX));
		int aliveTime = Integer.parseInt(valueMap.get(ALIVETIME));   
		int queueSize = Integer.parseInt(valueMap.get(QUEUESIZE));  
		pool = new TimedExecutorPool(core,max,aliveTime,new LinkedBlockingQueue<Runnable>(queueSize));
		pool.allowCoreThreadTimeOut(true);
		//Logger不支持序列化，只好放到这里
		this.log = LoggerFactory.getLogger(name); 
	}

	int setQueueSize(int queueSize){
		if(queueSize < 1 || queueSize > 10000000){
			queueSize = 200;
		}
		valueMap.put(QUEUESIZE, String.valueOf(queueSize));
		return queueSize;
	}

	void changeLogLevel(String level){
		if(log == null){
			return;
		}
		log.setLevel(Level.toLevel(level));
	}

	private boolean validKey(String key){
		boolean valid = THREADCORE.equals(key) || THREADMAX.equals(key) || ALIVETIME.equals(key) 
				|| OVERTIME.equals(key) || QUEUESIZE.equals(key) || CANCELLABLE.equals(key) || CRON.equals(key) ;
		return !valid;
	}

	/**
	 * 将key-value保存到valueMap中,可以在getValue或其他get中获取
	 * @param key key
	 * @param value value
	 * @throws UnsupportedActionException UnsupportedActionException
	 */
	public final void setValue(String key, String value) throws UnsupportedActionException{
		if(!validKey(key)){
			throw new UnsupportedActionException("不支持设置的key:" + key);
		}
		valueMap.put(key, value);
	}

	/**
	 * 通过key获取valueMap中的值
	 * @param key key
	 * @return value
	 */
	public final Object getValue(String key){
		return valueMap.get(key);
	}

	/**
	 * 获取valueMap中int值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public final int getInt(String key, int defaultValue){
		try{
			return Integer.parseInt(String.valueOf(valueMap.get(key)));
		}catch(Exception e){
			return defaultValue;
		}
	}

	/**
	 * 获取valueMap中long值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public final long getLong(String key, long defaultValue){
		try{
			return Long.parseLong(String.valueOf(valueMap.get(key)));
		}catch(Exception e){
			return defaultValue;
		}
	}

	/**
	 * 获取valueMap中boolean值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public final boolean getBoolean(String key, boolean defaultValue){
		try{
			return Boolean.parseBoolean(String.valueOf(valueMap.get(key)));
		}catch(Exception e){
			return defaultValue;
		}
	}

	/**
	 * 获取valueMap中string值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public final String getString(String key, String defaultValue){
		String value = String.valueOf(valueMap.get(key));
		if(StringUtils.isBlank(value)){
			return defaultValue;
		}
		return value;
	}

	/**
	 * 获取当前Context对象的name
	 * @return name name
	 */
	public final String getName(){
		return name;
	}

	/**
	 * 获取remark备注信息
	 * @return remark
	 */
	public final String getRemark(){
		return valueMap.get(REMARK);
	}

	/**
	 * 设置remark备注信息
	 * @param remark remark
	 */
	public final void setRemark(String remark){
		valueMap.put(REMARK, remark);
	}

	/**
	 * 获取本地线程池的核心线程数
	 * @return threadCore
	 */
	public final int getThreadCore(){
		return Integer.parseInt(valueMap.get(THREADCORE));
	}

	/**
	 * 设置本地线程池的核心线程数，将在下一个周期生效
	 * @param threadCore threadCore
	 * @return threadCore
	 */
	public final int setThreadCore(int threadCore){
		if(threadCore < 1 || threadCore > 10){
			threadCore = 4;
		}
		valueMap.put(THREADCORE, String.valueOf(threadCore));
		return threadCore;
	}

	/**
	 * 获取本地线程池的最大线程数
	 * @return threadMax
	 */
	public final int getThreadMax(){
		return Integer.parseInt(valueMap.get(THREADMAX));
	}

	/**
	 * 设置本地线程池的最大线程数，将在下一个周期生效
	 * @param threadMax threadMax
	 * @return threadMax
	 */
	public final int setThreadMax(int threadMax){
		if(threadMax < 10 || threadMax > 100){
			threadMax = 20;
		}
		valueMap.put(THREADMAX, String.valueOf(threadMax));
		return threadMax;
	}

	/**
	 * 获取本地线程池的线程存活时间
	 * @return aliveTime
	 */
	public final int getAliveTime(){
		return Integer.parseInt(valueMap.get(ALIVETIME));
	}

	/**
	 * 设置本地线程池的线程存活时间，将在下一个周期生效
	 * @param aliveTime aliveTime
	 * @return aliveTime
	 */
	public final int setAliveTime(int aliveTime){
		if(aliveTime < 3 || aliveTime > 600){
			aliveTime = 30;
		}
		valueMap.put(ALIVETIME, String.valueOf(aliveTime));
		return aliveTime;
	}

	/**
	 * 获取任务线程的超时时间
	 * @return overTime
	 */
	public final int getOverTime(){
		return Integer.parseInt(valueMap.get(OVERTIME));
	}

	/**
	 * 设置任务线程的超时时间，将在下一个周期生效
	 * @param overTime overTime
	 * @return overTime
	 */
	public final int setOverTime(int overTime){
		if(overTime < 60 || overTime > 86400){
			overTime = 3600;
		}
		valueMap.put(OVERTIME, String.valueOf(overTime));
		return overTime;
	}

	/**
	 * 获取cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @return cancellable
	 */
	public final boolean getCancellable(){
		return Boolean.parseBoolean(valueMap.get(CANCELLABLE));
	}

	/**
	 * 设置cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @param cancellable cancellable
	 * @return cancellable
	 */
	public final boolean setCancellable(boolean cancellable){
		valueMap.put(CANCELLABLE, String.valueOf(cancellable));
		return cancellable;
	}

	/**
	 * 获取context定时表达式
	 * @return cron
	 */
	public final String getCron(){
		return valueMap.get(CRON);
	}

	/**
	 * 设置context定时表达式，将在下一个周期生效
	 * @param cron cron
	 */
	public final void setCron(String cron){
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
	 * 0:初始化
	 * 1:启动 started
	 * 2:请求停止  waitting stop
	 * 3:已停止: stoped
	 */
	private int state = 0;

	/**
	 * 获取context状态
	 * @return state
	 */
	public final int state(){
		synchronized (name.intern()) {
			return state;
		}
	}

	/**
	 * 获取context状态
	 * @return state
	 */
	public final String stateString(){
		synchronized (name.intern()) {
			switch(state){
			case 0: return "inited";
			case 1: return "running";
			case 2: return "waitting stop";
			case 3: return "stoped";
			}
			return "";
		}
	}

	/**
	 * 启动context
	 * @return map(result/mag)
	 */
	public final Map<String,Object> start(){
		Map<String,Object> map = new HashMap<>();
		synchronized (name.intern()) {
			if(state == 1){
				log.warn("context[" + name + "] was already started."); 
				map.put("result", true);
				map.put("msg", "context[" + name + "] was already started.");
				return map;
			}
			if(state == 2){
				log.warn("context[" + name + "] is waitting stop, cann't start."); 
				map.put("result", false);
				map.put("msg", "context[" + name + "] is waitting stop, cann't start.");
				return map;
			}
			state = 1;
			innerThread = new InnerThread();
			innerThread.start();
		}
		log.info("context[" + name + "] started"); 
		map.put("result", true);
		map.put("msg", "context[" + name + "] started.");
		return map;
	}

	/**
	 * 停止context
	 * @return map(result/mag)
	 */
	public final Map<String,Object> stop(){
		Map<String,Object> map = new HashMap<>();
		synchronized (name.intern()) {
			if(state == 3){
				log.warn("context[" + name + "] was already stoped."); 
				map.put("result", true);
				map.put("msg", "context[" + name + "] was already stoped.");
				return map;
			}
			if(state == 0){
				log.warn("context[" + name + "] was not started, cann't stop."); 
				map.put("result", false);
				map.put("msg", "context[" + name + "] was not started, cann't stop.");
				return map;
			}
			if(state == 2){
				log.warn("context[" + name + "] is waitting stop, cann't stop."); 
				map.put("result", false);
				map.put("msg", "context[" + name + "] is waitting stop, cann't stop.");
				return map;
			}
			state = 2;
			innerThread.interrupt();//尽快响应
		}
		log.info("context[" + name + "] received stop request."); 
		map.put("result", true);
		map.put("msg", "context[" + name + "] received stop request.");
		return map;
	}

	/**
	 * 中断context
	 * @return map(result/mag)
	 */
	public final Map<String,Object> interrupt(){
		Map<String,Object> map = new HashMap<>();
		synchronized (name.intern()) {
			if(state != 1){
				log.warn("context[" + name + "] was not started, cann't interrupt."); 
				map.put("result", false);
				map.put("msg", "context[" + name + "] was not started, cann't interrupt.");
				return map;
			}
			innerThread.interrupt();
		}
		log.info("context[" + name + "] execute now."); 
		map.put("result", true);
		map.put("msg", "context[" + name + "] execute now.");
		return map;
	}

	private transient InnerThread innerThread;

	private class InnerThread extends Thread implements  Serializable {

		private static final long serialVersionUID = 2023244859604452982L;

		public InnerThread(){
			this.setName("context-" + name); 
		}

		@Override
		public void run() {
			startTime = System.currentTimeMillis();
			while(true){
				boolean isWaitingStop = false;
				synchronized (name.intern()) {
					isWaitingStop = state == 2;
				}
				if(isWaitingStop){
					pool.shutdownNow();
					try {
						pool.awaitTermination(1, TimeUnit.DAYS);
					} catch (InterruptedException e) {
						log.warn("interrupted when waiting stop."); 
					}
					cleanFutures();
					synchronized (name.intern()) {
						state = 3;
					}
					log.info("context[" + name + "]结束");
					return;
				}
				
				int threadCore = Integer.parseInt(valueMap.get(THREADCORE)); 
				if(pool.getCorePoolSize() != threadCore){
					pool.setCorePoolSize(threadCore);
				}

				int threadMax = Integer.parseInt(valueMap.get(THREADMAX)); 
				if(pool.getMaximumPoolSize() != threadMax){
					pool.setMaximumPoolSize(threadMax);
				}

				int threadAliveTime = Integer.parseInt(valueMap.get(ALIVETIME)); 
				if(pool.getKeepAliveTime(TimeUnit.SECONDS) != threadAliveTime){
					pool.setKeepAliveTime(threadAliveTime, TimeUnit.SECONDS);
				}

				cleanFutures();

				List<String> uriList = null;
				try {
					uriList = getUriList();
				} catch (Exception e) {
					log.error("", e); 
				}

				if(uriList != null){
					for (String sourceUri : uriList){
						if(isExecutorAlive(sourceUri)){
							continue;
						}
						try {
							Executor executor = createExecutor(sourceUri);
							executor.setName(name); 
							FUTUREMAP.put(sourceUri, pool.submit(executor)); 
							log.info("新建任务" + "[" + sourceUri + "]"); 
						} catch (RejectedExecutionException e) {
							log.warn("提交任务被拒绝,等待下次提交[" + sourceUri + "].");
							break;
						}catch (Exception e) {
							log.error("新建任务异常[" + sourceUri + "]", e); 
						}
					}
				}
				if(cronExpression == null){
					//默认只执行一次，执行完便停止，等待提交的线程结束
					synchronized (name.intern()) {
						state = 2;
					}
					pool.shutdown();
					try {
						pool.awaitTermination(1, TimeUnit.DAYS);
					} catch (InterruptedException e) {
						log.warn("interrupted when waiting stop."); 
					}
					cleanFutures();
					synchronized (name.intern()) {
						state = 3;
					}
					log.info("context[" + name + "]结束");
					return;
				}
				Date nextTime = cronExpression.getTimeAfter(new Date());
				long waitTime = nextTime.getTime() - System.currentTimeMillis();
				synchronized (this) {
					try {
						wait(waitTime);
					} catch (InterruptedException e) {
						//借助interrupted标记来重启
						log.info("wait interrupted."); 
					}
				}
			}
		}
	}

	/**
	 * 返回资源uri列表，context将根据每个uri创建一个Executor执行器提交到线程池
	 * @return List sourceUri list
	 * @throws Exception Exception
	 */
	protected abstract List<String> getUriList() throws Exception;

	/**
	 * 根据uri创建一个Executor的具体实例
	 * @param sourceUri 资源uri
	 * @return Executor
	 * @throws Exception Exception
	 */
	protected abstract Executor createExecutor(String sourceUri) throws Exception;

	private void cleanFutures(){
		Iterator<Map.Entry<String, TimedFuture<Result>>> it = FUTUREMAP.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, TimedFuture<Result>> entry = it.next();
			TimedFuture<Result> future = entry.getValue();
			if(!name.equals(future.getName())){   
				continue;
			}
			String sourceUri = entry.getKey();
			if(!future.isDone()){
				long existTime = (System.currentTimeMillis() - future.getCreateTime()) / 1000;
				int threadOverTime = Integer.parseInt(valueMap.get(OVERTIME)); 
				if(existTime > threadOverTime) {
					log.warn("任务超时[" + sourceUri + "]," + existTime + "s");
					if(Boolean.parseBoolean(valueMap.get(CANCELLABLE))) { 
						future.cancel(true);
					}
				}
				continue;
			}

			try {
				future.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();//保留中断标记
			} catch (ExecutionException e) {
				log.error("", e); //result handler exception
			}
			it.remove();
		}
	}

	/**
	 * null 没有创建过任务
	 * done 创建过任务，但远程文件没删除
	 * else 任务还没结束
	 */
	private boolean isExecutorAlive(String key){
		Future<Result> future = FUTUREMAP.get(key);
		return future != null && !future.isDone();
	}
}
