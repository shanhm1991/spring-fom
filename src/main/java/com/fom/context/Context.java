package com.fom.context;

import static com.fom.context.State.inited;
import static com.fom.context.State.running;
import static com.fom.context.State.sleeping;
import static com.fom.context.State.stopped;
import static com.fom.context.State.stopping;
import static com.fom.context.State.waiting;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 模块最小单位，相当于一个组织者的角色，负责创建和组织Task的运行
 * 
 * @author shanhm
 *
 */
public class Context implements Serializable {

	private static final long serialVersionUID = 9154119563307298882L;

	//所有的Context共用，防止两个Context创建针对同一个文件的任务
	private static final Map<String,TimedFuture<Result<?>>> FUTUREMAP = new ConcurrentHashMap<>(1000);
	
	protected final ContextConfig config = new ContextConfig();

	protected final String name;

	protected transient Logger log;

	volatile transient long loadTime;

	volatile transient long execTime;

	transient ContextStatistics statistics;

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

	/**
	 * xml > 注解  > 默认
	 * @param name
	 * @param fc
	 */
	private void initValue(String name,FomContext fc){
		this.log = LoggerFactory.getLogger(name); 
		config.init(ContextManager.elementMap.get(name),  ContextManager.createMap.get(name), fc);
		statistics = new ContextStatistics();
		loadTime = System.currentTimeMillis();
	}

	/**
	 * 注册到容器
	 */
	public void regist() {
		ContextManager.register(this); 
	}

	void unSerialize(){
		this.log = LoggerFactory.getLogger(name); 
		switchState(inited);
		config.initPool();
		statistics = new ContextStatistics();
		loadTime = System.currentTimeMillis();
	}
	
	/**
	 * 获取正在执行的任务数
	 * @return 正在执行的任务数
	 */
	public final long getActives(){
		return config.getActives();
	}

	/**
	 * 获取等待队列中的任务数
	 * @return 等待队列中的任务数
	 */
	public final int getWaitings(){
		return config.getWaitings();
	}

	/**
	 * 获取等待队列中任务的id-createTime
	 * @return map
	 */
	public final Map<String, Object> getWaitingDetail(){
		return config.getWaitingDetail();
	}

	/**
	 * 获取所有创建过的任务数
	 * @return 所有创建过的任务数
	 */
	public final long getCreated(){
		return config.getCreated();
	}

	/**
	 * 获取已完成的任务数
	 * @return 已完成的任务数
	 */
	public final long getCompleted(){
		return config.getCompleted();
	}

	/**
	 * 获取正在执行任务的Thead
	 * @return taskId-Thread
	 */
	public final Map<Task, Thread> getActiveThreads() {
		return config.getActiveThreads();
	}

	/**
	 * 获取成功的任务数
	 * @return 成功的任务数
	 */
	public final long getSuccess(){
		return statistics.getSuccess();
	}

	/**
	 * 获取失败的任务数
	 * @return 失败的任务数
	 */
	public final long getFailed(){
		return statistics.getFailed();
	}

	/**
	 * 修改日志级别
	 * @param level level
	 */
	public final void changeLogLevel(String level){
		if(log == null){
			return;
		}
		log.setLevel(Level.toLevel(level));
	}

	/**
	 * 获取当前本地context的名称
	 * @return name name
	 */
	public final String getName(){
		return name;
	}

	private transient State state = inited; 

	private void switchState(State s) {
		synchronized (name.intern()) {
			state = s;
		}
	}

	/**
	 * 获取context状态
	 * @return state
	 */
	public final State getState(){
		synchronized (name.intern()) {
			if(state == stopping && config.pool.isTerminated()){
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
				if(config.pool.isShutdown()){
					config.initPool();
				}
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
					if(innerThread.isAlive()){
						innerThread.interrupt();//尽快响应
						map.put("result", true);
						map.put("msg", "context[" + name + "] is stopping.");
						log.info("context[" + name + "] is stopping."); 
						return map;
					}else{
						state = stopped;
						map.put("result", true);
						map.put("msg", "context[" + name + "] stopped.");
						log.info("context[" + name + "] stopped."); 
						return map;
					}
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

				switchState(running);
				execTime = System.currentTimeMillis();
				try {
					Set<Task> tasks = scheduleBatchTasks();
					if(tasks != null){
						for (Task task : tasks){
							String taskId = task.getId();
							if(isTaskAlive(taskId)){ 
								if (log.isDebugEnabled()) {
									log.debug("task[" + taskId + "] is still alive, create canceled"); 
								}
								continue;
							}
							submit(task);
						}
					}
				} catch (RejectedExecutionException e) {
					log.warn("task submit rejected.");
				} catch (Exception e){
					log.error("get task failed", e);
				}

				if(config.cronExpression != null) {
					Date nextTime = config.cronExpression.getTimeAfter(new Date(execTime)); 
					long waitTime = nextTime.getTime() - System.currentTimeMillis();
					//如果设定周期较短，而执行时间较长
					if(waitTime > 0){
						switchState(sleeping);
						synchronized (this) {
							try {
								wait(waitTime);
							} catch (InterruptedException e) {
								//借助interrupted标记来重启
								log.info("sleep interrupted."); 
							}
						}
					}
				}else{
					if(config.getStopWithNoCron()){
						terminate();
					}
					return;
				}
			}
		}
	}

	private void stop(){
		config.pool.shutdownNow();
		boolean stopSucc = false;
		try {
			stopSucc = config.pool.awaitTermination(1, TimeUnit.DAYS);
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
		switchState(waiting);
		config.pool.shutdown();

		if(waitTask()){
			onScheduleTerminate();
			switchState(stopped);
		}

		cleanFutures();
	}

	private boolean waitTask(){
		boolean isStopping = false;
		while(true){
			try {
				if(config.pool.awaitTermination(1, TimeUnit.DAYS)){
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
					config.pool.shutdownNow();
				}
			}
		}
	}

	private AtomicLong submits = new AtomicLong(0); 

	/**
	 * 提交任务
	 * @param task task
	 * @return TimedFuture
	 * @throws Exception Exception
	 */
	public final TimedFuture<Result<?>> submit(Task task) throws Exception {
		if(submits.incrementAndGet() % 1000 == 0){
			cleanFutures();
		}
		String taskId = task.getId();
		task.setContext(Context.this); 
		TimedFuture<Result<?>> future = config.pool.submit(task);
		FUTUREMAP.put(taskId, future); 
		log.info("task[" + taskId + "] submited."); 
		return future; 
	}

	/**
	 * 周期性获取批量任务
	 * @return task set
	 * @throws Exception Exception
	 */
	protected Set<Task> scheduleBatchTasks() throws Exception {
		return null;
	}

	/**
	 * 一次性批量任务全部完成时执行的操作
	 */
	protected void onScheduleTerminate() {

	}

	/**
	 * null 没有创建过任务
	 * done 创建过任务，但远程文件没删除
	 * else 任务还没结束
	 */
	private boolean isTaskAlive(String key){
		Future<Result<?>> future = FUTUREMAP.get(key);
		return future != null && !future.isDone();
	}

	private void cleanFutures(){
		new Thread(name + "-clean"){
			@Override
			public void run() {
				Iterator<Map.Entry<String, TimedFuture<Result<?>>>> it = FUTUREMAP.entrySet().iterator();
				while(it.hasNext()){
					Entry<String, TimedFuture<Result<?>>> entry = it.next();
					TimedFuture<Result<?>> future = entry.getValue();
					if(!name.equals(future.getContextName())){   
						continue;
					}
					String taskId = entry.getKey(); 
					if(future.getStartTime() > 0 && !future.isDone()){
						long existTime = (System.currentTimeMillis() - future.getStartTime()) / 1000;
						int threadOverTime = Integer.parseInt(config.get(ContextConfig.OVERTIME)); 
						if(existTime > threadOverTime) {
							log.warn("task overtime[" + taskId + "]," + existTime + "s");
							if(Boolean.parseBoolean(config.get(ContextConfig.CANCELLABLE))) { 
								future.cancel(true);
							}
						}
						continue;
					}

					it.remove();
				}
			}
		}.start();

	}
}
