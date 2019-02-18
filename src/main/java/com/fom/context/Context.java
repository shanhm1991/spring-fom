package com.fom.context;

import static com.fom.context.State.inited;
import static com.fom.context.State.running;
import static com.fom.context.State.sleeping;
import static com.fom.context.State.stopped;
import static com.fom.context.State.stopping;
import static com.fom.context.State.waiting;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Collection;
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.quartz.CronExpression;

import com.fom.log.LoggerFactory;
import com.fom.util.XmlUtil;

/**
 * 模块最小单位，相当于一个组织者的角色，负责创建和组织Task的运行
 * 
 * @author shanhm
 *
 */
public abstract class Context implements Serializable {

	private static final long serialVersionUID = 9154119563307298882L;

	//所有的Context共用，防止两个Context创建针对同一个文件的任务
	private static final Map<String,TimedFuture<Result>> FUTUREMAP = new ConcurrentHashMap<>(500);

	protected final String name;

	protected transient Logger log;

	volatile long loadTime;

	volatile long execTime;

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

	/**
	 * @param name 模块名称
	 */
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
			setThreadCore(XmlUtil.getInt(element, Constants.THREADCORE, 4, 1, 10));
			setThreadMax(XmlUtil.getInt(element, Constants.THREADMAX, 20, 10, 100));
			setAliveTime(XmlUtil.getInt(element, Constants.ALIVETIME, 30, 5, 300));
			setOverTime(XmlUtil.getInt(element, Constants.OVERTIME, 3600, 60, 86400));
			setQueueSize(XmlUtil.getInt(element, Constants.QUEUESIZE, 200, 1, 10000000));
			setCancellable(XmlUtil.getBoolean(element, Constants.CANCELLABLE, false));
			setCron(XmlUtil.getString(element, Constants.CRON, ""));
			setRemark(XmlUtil.getString(element, Constants.REMARK, ""));
		}else if(cMap != null){
			valueMap.putAll(cMap);
			setThreadCore(getInt(cMap, Constants.THREADCORE));
			setThreadMax(getInt(cMap, Constants.THREADMAX));
			setAliveTime(getInt(cMap, Constants.ALIVETIME));
			setOverTime(getInt(cMap, Constants.OVERTIME));
			setQueueSize(getInt(cMap, Constants.QUEUESIZE));
			setCancellable(getBoolean(cMap, Constants.CANCELLABLE));
			setCron(cMap.get(Constants.CRON)); 
			setRemark(cMap.get(Constants.REMARK));
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
		loadTime = System.currentTimeMillis();
	}
	
	public void regist() {
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
		int core = Integer.parseInt(valueMap.get(Constants.THREADCORE)); 
		int max = Integer.parseInt(valueMap.get(Constants.THREADMAX));
		int aliveTime = Integer.parseInt(valueMap.get(Constants.ALIVETIME));   
		int queueSize = Integer.parseInt(valueMap.get(Constants.QUEUESIZE));  
		pool = new TimedExecutorPool(core,max,aliveTime,new LinkedBlockingQueue<Runnable>(queueSize));
		pool.allowCoreThreadTimeOut(true);
		//Logger不支持序列化，只好放到这里
		this.log = LoggerFactory.getLogger(name); 
	}

	/**
	 * 获取正在执行的任务数
	 * @return 正在执行的任务数
	 */
	public final long getActives(){
		if(pool == null){
			return 0;
		}
		return pool.getActiveCount();
	}

	/**
	 * 获取等待队列中的任务数
	 * @return 等待队列中的任务数
	 */
	public final int getWaitings(){
		return pool.getQueue().size();
	}
	
	/**
	 * 获取所有创建过的任务数
	 * @return 所有创建过的任务数
	 */
	public final long getCreated(){
		if(pool == null){
			return 0;
		}
		return pool.getTaskCount();
	}

	/**
	 * 获取已完成的任务数
	 * @return 已完成的任务数
	 */
	public final long getCompleted(){
		if(pool == null){
			return 0;
		}
		return pool.getCompletedTaskCount();
	}

	@SuppressWarnings("unchecked")
	Collection<Thread> getActiveThreads() {
		if(pool == null){
			return CollectionUtils.EMPTY_COLLECTION;
		}
		return pool.getActiveThreads();
	}

	int setQueueSize(int queueSize){
		if(queueSize < 1 || queueSize > 10000000){
			queueSize = 200;
		}
		valueMap.put(Constants.QUEUESIZE, String.valueOf(queueSize));
		return queueSize;
	}

	void changeLogLevel(String level){
		if(log == null){
			return;
		}
		log.setLevel(Level.toLevel(level));
	}

	/**
	 * 将key-value保存到本地context的valueMap中
	 * @param key key
	 * @param value value
	 */
	public final void setValue(String key, String value) {
		if(!Constants.validKey(key)){
			throw new IllegalArgumentException("not support key:" + key);
		}
		valueMap.put(key, value);
	}

	/**
	 * 通过key获取valueMap中的值
	 * @param key key
	 * @return value
	 */
	public final String getValue(String key){
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
	 * 获取当前本地context的名称
	 * @return name name
	 */
	public final String getName(){
		return name;
	}

	/**
	 * 获取本地context的备注信息
	 * @return remark
	 */
	public final String getRemark(){
		return valueMap.get(Constants.REMARK);
	}

	/**
	 * 设置remark备注信息
	 * @param remark remark
	 */
	public final void setRemark(String remark){
		valueMap.put(Constants.REMARK, remark);
	}

	/**
	 * 获取本地线程池的核心线程数
	 * @return threadCore
	 */
	public final int getThreadCore(){
		return Integer.parseInt(valueMap.get(Constants.THREADCORE));
	}

	/**
	 * 设置本地线程池的核心线程数，将在下一次运行生效
	 * @param threadCore threadCore
	 * @return threadCore
	 */
	public final int setThreadCore(int threadCore){
		if(threadCore < 1 || threadCore > 10){
			threadCore = 4;
		}
		valueMap.put(Constants.THREADCORE, String.valueOf(threadCore));
		if(pool != null && pool.getCorePoolSize() != threadCore){
			pool.setCorePoolSize(threadCore);
		}
		return threadCore;
	}

	/**
	 * 获取本地线程池的最大线程数
	 * @return threadMax
	 */
	public final int getThreadMax(){
		return Integer.parseInt(valueMap.get(Constants.THREADMAX));
	}

	/**
	 * 设置本地线程池的最大线程数，将在下一次运行生效
	 * @param threadMax threadMax
	 * @return threadMax
	 */
	public final int setThreadMax(int threadMax){
		if(threadMax < 10 || threadMax > 100){
			threadMax = 20;
		}
		valueMap.put(Constants.THREADMAX, String.valueOf(threadMax));
		if(pool != null && pool.getMaximumPoolSize() != threadMax){
			pool.setMaximumPoolSize(threadMax);
		}
		return threadMax;
	}

	/**
	 * 获取本地线程池的线程存活时间
	 * @return aliveTime
	 */
	public final int getAliveTime(){
		return Integer.parseInt(valueMap.get(Constants.ALIVETIME));
	}

	/**
	 * 设置本地线程池的线程存活时间，将在下一次运行生效
	 * @param aliveTime aliveTime
	 * @return aliveTime
	 */
	public final int setAliveTime(int aliveTime){
		if(aliveTime < 3 || aliveTime > 600){
			aliveTime = 30;
		}
		valueMap.put(Constants.ALIVETIME, String.valueOf(aliveTime));
		if(pool != null && pool.getKeepAliveTime(TimeUnit.SECONDS) != aliveTime){
			pool.setKeepAliveTime(aliveTime, TimeUnit.SECONDS);
		}
		return aliveTime;
	}

	/**
	 * 获取任务线程的超时时间
	 * @return overTime
	 */
	public final int getOverTime(){
		return Integer.parseInt(valueMap.get(Constants.OVERTIME));
	}

	/**
	 * 设置任务线程的超时时间，将在下一次运行生效
	 * @param overTime overTime
	 * @return overTime
	 */
	public final int setOverTime(int overTime){
		if(overTime < 60 || overTime > 86400){
			overTime = 3600;
		}
		valueMap.put(Constants.OVERTIME, String.valueOf(overTime));
		return overTime;
	}

	/**
	 * 获取cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @return cancellable
	 */
	public final boolean getCancellable(){
		return Boolean.parseBoolean(valueMap.get(Constants.CANCELLABLE));
	}

	/**
	 * 设置cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @param cancellable cancellable
	 * @return cancellable
	 */
	public final boolean setCancellable(boolean cancellable){
		valueMap.put(Constants.CANCELLABLE, String.valueOf(cancellable));
		return cancellable;
	}

	/**
	 * 获取context定时表达式
	 * @return cron
	 */
	public final String getCron(){
		return valueMap.get(Constants.CRON);
	}

	/**
	 * 设置context定时表达式
	 * <br>如果之前存在，将在下一次运行生效，否则需要重新激活（startup）才能生效
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
				|| !(cron.equals(valueMap.get(Constants.CRON)  ))){
			valueMap.put(Constants.CRON, cron);
			cronExpression = c;
		}
	}


	private transient State state = inited; 

	/**
	 * 获取context状态
	 * @return state
	 */
	public final State getState(){
		synchronized (name.intern()) {
			if(state == stopping && pool.getActiveCount() == 0){
				state = stopped;
			}
			return state;
		}
	}

	/**
	 * 启动
	 * @return map(result/mag)
	 */
	public final Map<String,Object> startup(){
		Map<String,Object> map = new HashMap<>();
		synchronized (name.intern()) {
			switch(state){
			case inited:
			case stopped:
				state = running;
				innerThread = new InnerThread();
				innerThread.start();
				map.put("result", true);
				map.put("msg", "context[" + name + "] started.");
				log.info("context[" + name + "] started"); 
				return map;
			case stopping:
				map.put("result", false);
				map.put("msg", "context[" + name + "] is stopping, cann't start.");
				log.warn("context[" + name + "] is stopping, cann't start."); 
				return map;
			case running:
			case waiting:
			case sleeping:
				map.put("result", true);
				map.put("msg", "context[" + name + "] was already started.");
				log.warn("context[" + name + "] was already started."); 
				return map;
			default:
				map.put("result", false);
				map.put("msg", "invalid state.");
				return map;
			}
		}
	}

	/**
	 * 停止
	 * @return map(result/mag)
	 */
	public final Map<String,Object> shutDown(){
		Map<String,Object> map = new HashMap<>();
		synchronized (name.intern()) {
			synchronized (name.intern()) {
				switch(state){
				case inited:
				case stopped:
					map.put("result", true);
					map.put("msg", "context[" + name + "] was already stoped.");
					log.warn("context[" + name + "] was already stoped."); 
					return map;
				case stopping:
					map.put("result", false);
					map.put("msg", "context[" + name + "] is stopping, cann't stop.");
					log.warn("context[" + name + "] is stopping, cann't stop."); 
					return map;
				case running:
				case waiting:
				case sleeping:
					state = stopping;
					innerThread.interrupt();//尽快响应
					map.put("result", true);
					map.put("msg", "context[" + name + "] is stopping.");
					log.info("context[" + name + "] is stopping."); 
					return map;
				default:
					map.put("result", false);
					map.put("msg", "invalid state.");
					return map;
				}
			}
		}
	}

	/**
	 * 立即运行（中断等待）
	 * @return map(result/mag)
	 */
	public final Map<String,Object> execNow(){
		Map<String,Object> map = new HashMap<>();
		synchronized (name.intern()) {
			switch(state){
			case inited:
			case stopped:
				map.put("result", false);
				map.put("msg", "context[" + name + "] was already stoped, cann't execut now.");
				log.warn("context[" + name + "] isn't running, cann't execut now."); 
				return map;
			case stopping:
				map.put("result", false);
				map.put("msg", "context[" + name + "] is stopping, cann't execut now.");
				log.warn("context[" + name + "] is stopping, cann't execut now."); 
				return map;
			case running:
				map.put("result", false);
				map.put("msg", "context[" + name + "] is already executing.");
				log.info("context[" + name + "] is alrady executing."); 
				return map;
			case waiting:
				map.put("result", false);
				map.put("msg", "context[" + name + "] is waiting for task completion.");
				log.info("context[" + name + "] is waiting for task completion."); 
				return map;
			case sleeping:
				innerThread.interrupt();
				map.put("result", true);
				map.put("msg", "context[" + name + "] execute now.");
				log.info("context[" + name + "] execute now."); 
				return map;
			default:
				map.put("result", false);
				map.put("msg", "invalid state.");
				return map;
			}
		}
	}

	private transient InnerThread innerThread;

	private class InnerThread extends Thread implements  Serializable {

		private static final long serialVersionUID = 2023244859604452982L;

		public InnerThread(){
			this.setName("context[" + name + "]"); 
		}

		@Override
		public void run() {
			while(true){
				boolean isStopping = false;
				synchronized (name.intern()) {
					isStopping = state == stopping;
				}
				if(isStopping){
					Context.this.stop();
					return;
				}

				synchronized (name.intern()) {
					state = running;
				}
				execTime = System.currentTimeMillis();

				cleanFutures();

				List<String> uriList = null;
				try {
					uriList = getTaskIdList();
				} catch (Exception e) {
					log.error("", e); 
				}

				if(uriList != null){
					for (String sourceUri : uriList){
						if(isExecutorAlive(sourceUri)){
							continue;
						}
						try {
							Task executor = createTask(sourceUri);
							executor.setContext(name); 
							FUTUREMAP.put(sourceUri, pool.submit(executor)); 
							log.info("create task" + "[" + sourceUri + "]"); 
						} catch (RejectedExecutionException e) {
							log.warn("task submit rejected, will try next time[" + sourceUri + "].");
							break;
						}catch (Exception e) {
							log.error("create task failed[" + sourceUri + "]", e); 
						}
					}
				}

				//默认只执行一次，提交后等待任务线程完成
				if(cronExpression == null){
					terminate();
					return;
				}

				Date nextTime = cronExpression.getTimeAfter(new Date(execTime)); 
				long waitTime = nextTime.getTime() - System.currentTimeMillis();
				//如果设定周期较短，而执行时间较长
				if(waitTime > 0){
					synchronized (name.intern()) {
						state = sleeping;
					}
					synchronized (this) {
						try {
							wait(waitTime);
						} catch (InterruptedException e) {
							//借助interrupted标记来重启
							log.info("sleep interrupted."); 
						}
					}
				}
			}
		}
	}

	private void stop(){
		pool.shutdownNow();
		boolean stopSucc = false;
		try {
			stopSucc = pool.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			log.error("interrupted when stopping, which should never happened."); 
		}
		cleanFutures();
		synchronized (name.intern()) {
			if(stopSucc){
				state = stopped;
				log.info("context[" + name + "] stoped.");
			}else{
				log.warn("context[" + name + "] is still stopping, though has waiting for a day.");
			}
		}
	}

	private void terminate(){
		synchronized (name.intern()) {
			state = waiting;
		}
		pool.shutdown();
		
		if(waitTask()){
			synchronized (name.intern()) {
				state = stopped;
			}
		}

		cleanFutures();
	}

	private boolean waitTask(){
		boolean isStopping = false;
		while(true){
			try {
				if(pool.awaitTermination(1, TimeUnit.DAYS)){
					log.info("context[" + name + "] stoped.");
					return true;
				}else if(isStopping){
					log.warn("context[" + name + "] is still stopping, though has waiting for a day.");
					return false;
				}
			} catch (InterruptedException e) {
				log.warn("interrupted when waiting executing task."); 
			}

			synchronized (name.intern()) {
				if(state == stopping){
					isStopping = true;
					pool.shutdownNow();
				}
			}
		}
	}

	/**
	 * 返回资源uri列表，context将根据每个uri创建一个Executor执行器提交到线程池
	 * @return List taskId list
	 * @throws Exception Exception
	 */
	protected abstract List<String> getTaskIdList() throws Exception;

	/**
	 * 根据uri创建一个Executor的具体实例
	 * @param executorId 资源uri
	 * @return Executor
	 * @throws Exception Exception
	 */
	protected abstract Task createTask(String executorId) throws Exception;

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
				int threadOverTime = Integer.parseInt(valueMap.get(Constants.OVERTIME)); 
				if(existTime > threadOverTime) {
					log.warn("task overtime[" + sourceUri + "]," + existTime + "s");
					if(Boolean.parseBoolean(valueMap.get(Constants.CANCELLABLE))) { 
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
