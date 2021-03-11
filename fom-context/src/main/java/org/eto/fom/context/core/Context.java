package org.eto.fom.context.core;

import static org.eto.fom.context.core.State.INITED;
import static org.eto.fom.context.core.State.RUNNING;
import static org.eto.fom.context.core.State.SLEEPING;
import static org.eto.fom.context.core.State.STOPPED;
import static org.eto.fom.context.core.State.STOPPING;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.eto.fom.context.Monitor;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.annotation.SchedulCompleter;
import org.eto.fom.context.annotation.SchedulFactory;
import org.eto.fom.context.annotation.SchedulTerminator;
import org.eto.fom.util.log.SlfLoggerFactory;
import org.slf4j.Logger;
import org.springframework.util.Assert;

/**
 * 模块最小单位，相当于一个组织者的角色，负责创建和组织Task的运行
 * 
 * @author shanhm
 * 
 * @param <E> 任务执行结果类型
 *
 */
public class Context<E> implements SchedulFactory<E>, SchedulCompleter<E>, SchedulTerminator {

	private static final int SECOND_UNIT = 1000;

	// 当前构造的context的name
	static final ThreadLocal<String> localName = new ThreadLocal<>();

	// 所有的任务提交总数
	private static final AtomicLong SUBMITS = new AtomicLong();

	// 所有Context共用，以便根据id实现任务冲突检测
	private static final Map<String,TimedFuture<Result<?>>> SUBMITMAP = new HashMap<>(1024);

	// 当前Context当前提交的任务Future
	private List<TimedFuture<Result<E>>> submitFutures = new ArrayList<>();

	// 批任务提交次数，与定时任务无关，记录外部线程主动批任务提交次数
	private final AtomicLong batchSubmits = new AtomicLong();

	// context加载时间
	final long loadTime = System.currentTimeMillis();

	// 任务统计信息
	private final ContextStatistics statistics = new ContextStatistics();

	// context名称
	protected final String name;

	// context配置
	protected final ContextConfig config;

	// 日志根据name初始化
	protected Logger log;

	// 最近一次定时任务执行时间
	volatile long lastTime;

	// 下次定时任务执行时间
	volatile long nextTime;

	// 定时任务执行次数
	volatile long schedulTimes;

	// 是否是启动后的第一次执行
	private boolean isFirstRun = true;

	// 状态
	private State state = INITED; 

	public Context(){
		String lname = localName.get();
		if(StringUtils.isNotBlank(lname)){
			this.name = lname;
		}else{
			Class<?> clazz = this.getClass();
			FomContext fc = clazz.getAnnotation(FomContext.class);
			if(fc != null && StringUtils.isNotBlank(fc.name())){
				this.name = fc.name();
			}else{
				this.name = clazz.getSimpleName();
			}
		}
		this.config = new ContextConfig(name);
		this.log = SlfLoggerFactory.getLogger(name); 
	}

	public Context(String name){
		if(StringUtils.isNotBlank(name)){
			this.name = name;
		}else{
			Class<?> clazz = this.getClass();
			FomContext fc = clazz.getAnnotation(FomContext.class);
			if(fc != null && StringUtils.isNotBlank(fc.name())){
				this.name = fc.name();
			}else{
				this.name = clazz.getSimpleName();
			}
		}
		this.config = new ContextConfig(name);
		this.log = SlfLoggerFactory.getLogger(name); 
	}

	/**
	 * 注册自己
	 * @param putConfig
	 * @throws Exception
	 */
	void regist(boolean putConfig) throws Exception {
		ContextManager.register(this, putConfig);  
	}

	public final ContextConfig getConfig(){
		return config;
	}

	public final ContextStatistics getStatistics(){
		return statistics;
	}

	/**
	 * 修改日志级别
	 * @param level level
	 */
	public final void changeLogLevel(String level){
		if(log == null){
			return;
		}
		org.apache.log4j.Logger logger = LogManager.exists(name);
		logger.setLevel(Level.toLevel(level));
	}

	/**
	 * 获取当前日志级别
	 * @return 级别
	 */
	public final String getLogLevel() {
		if(log == null){
			throw new NullPointerException();
		}
		org.apache.log4j.Logger logger = LogManager.exists(name);
		Level level = logger.getLevel();
		if(level == null){
			return "INFO";
		}
		return level.toString();
	}

	/**
	 * 获取当前本地context的名称
	 * @return name name
	 */
	public final String getName(){
		return name;
	}

	private void switchState(State s) {
		synchronized (this) {
			state = s;
		}
	}

	/**
	 * 获取context状态
	 * @return state
	 */
	public final State getState(){
		synchronized (this) {
			if(state == STOPPING && config.pool.isTerminated()){
				state = STOPPED;
				isFirstRun = true;
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
		synchronized (this) {
			switch(state){
			case INITED:
			case STOPPED:
				if(config.pool.isShutdown()){
					config.init();
				}
				innerThread = new InnerThread();
				innerThread.start();
				map.put("result", true);
				map.put("msg", "context[" + name + "] startup.");
				log.info("context[{}] startup", name); 
				return map;
			case STOPPING:
				map.put("result", false);
				map.put("msg", "context[" + name + "] is stopping, cann't startup.");
				log.warn("context[{}] is stopping, cann't startup.", name); 
				return map;
			case RUNNING:
			case SLEEPING:
				map.put("result", true);
				map.put("msg", "context[" + name + "] was already startup.");
				log.warn("context[{}] was already startup.", name); 
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
		synchronized (this) {
			switch(state){
			case INITED:
				map.put("result", false);
				map.put("msg", "context[" + name + "] was not startup.");
				log.warn("context[{}] was not startup.", name); 
				return map;
			case STOPPED:
				map.put("result", true);
				map.put("msg", "context[" + name + "] was already stopped.");
				log.warn("context[{}] was already stopped.", name); 
				return map;
			case STOPPING:
				map.put("result", false);
				map.put("msg", "context[" + name + "] is stopping, cann't stop.");
				log.warn("context[{}] is stopping, cann't stop.", name); 
				return map;
			case RUNNING:
			case SLEEPING:
				state = STOPPING;
				if(innerThread.isAlive()){
					innerThread.interrupt();//尽快响应
					map.put("result", true);
					map.put("msg", "context[" + name + "] is stopping.");
					log.info("context[{}] is stopping.", name);
				}else{
					state = STOPPED;
					isFirstRun = true;
					map.put("result", true);
					map.put("msg", "context[" + name + "] stopped.");
					log.info("context[{}] stopped.", name);
				}
				return map;
			default:
				map.put("result", false);
				map.put("msg", "invalid state.");
				return map;
			}
		}
	}

	/**
	 * 立即运行（中断等待）
	 * @return map(result/mag)
	 */
	public final Map<String,Object> execNow(){
		Map<String,Object> map = new HashMap<>();
		synchronized (this) {
			switch(state){
			case INITED:
			case STOPPED:
				map.put("result", false);
				map.put("msg", "context[" + name + "] was not startup, cann't execut now.");
				log.warn("context[{}] was not startup, cann't execut now.", name); 
				return map;
			case STOPPING:
				map.put("result", false);
				map.put("msg", "context[" + name + "] is stopping, cann't execut now.");
				log.warn("context[{}] is stopping, cann't execut now.", name); 
				return map;
			case RUNNING:
				map.put("result", false);
				map.put("msg", "context[" + name + "] is executing, and will re-executr immediately after completion .");
				log.info("context[{}] is executing, and will re-executr immediately after completion .", name); 
				return map;
			case SLEEPING:
				innerThread.interrupt();
				map.put("result", true);
				map.put("msg", "context[" + name + "] execute now.");
				log.info("context[{}] execute now.", name); 
				return map;
			default:
				map.put("result", false);
				map.put("msg", "invalid state.");
				return map;
			}
		}
	}

	private InnerThread innerThread;

	private class InnerThread extends Thread implements  Serializable {

		private static final long serialVersionUID = 2023244859604452982L;

		public InnerThread(){
			this.setName("context[" + name + "]"); 
		}

		@Override
		public void run() {
			while(true){
				boolean isStopping;
				synchronized (Context.this) {
					isStopping = state == STOPPING;
				}
				if(isStopping){
					terminate();
					return;
				}

				try{
					if(isFirstRun && !config.getExecOnLoad()){
						isFirstRun = false;
						caculateNextTime(null); 
					}else{
						runSchedul();
					}
				}catch(InterruptedException e){
					// ignore 进入下次循环，获取判断当前状态
				}catch (RuntimeException e){
					log.error("", e);
				}catch(Throwable e){
					log.error("context[" + name + "] terminated unexpectedly", e); 
					terminate();
					return;
				}

				if(nextTime > 0) {
					long waitTime = nextTime - System.currentTimeMillis();
					if(!Thread.interrupted() && waitTime > 0){ 
						switchState(SLEEPING);
						synchronized (this) {
							try {
								wait(waitTime);
							} catch (InterruptedException e) {
								//借助interrupted标记来中断睡眠，立即重新执行
							}
						}
					}
				}else{
					synchronized(Context.this){
						if(state == INITED){
							return;
						}
					}

					terminate(); 
					return;
				}
			}
		}

		private void caculateNextTime(ScheduleBatch<E> scheduleBatch) { 
			Date last = new Date();
			if(lastTime > 0){
				last = new Date(lastTime);
			}

			if(config.cronExpression != null){
				nextTime = config.cronExpression.getTimeAfter(last).getTime();
				waitTaskCompleted(scheduleBatch);
			}else if(config.getFixedRate() > 0){
				nextTime = last.getTime() + config.getFixedRate() * SECOND_UNIT;
				waitTaskCompleted(scheduleBatch);
			}else if(config.getFixedDelay() > 0){
				waitTaskCompleted(scheduleBatch);
				nextTime = System.currentTimeMillis() + config.getFixedDelay() * SECOND_UNIT;
			}else{
				waitTaskCompleted(scheduleBatch);
				nextTime = 0;
			}
		}

		private void waitTaskCompleted(ScheduleBatch<E> scheduleBatch){
			if(scheduleBatch == null){
				return;
			}

			try {
				long overTime = config.getOverTime();
				while(true){
					if(scheduleBatch.waitTaskCompleted(overTime)){ 
						cleanCompletedFutures();
						return;
					}else if(config.getCancellable()){
						for(TimedFuture<Result<E>> future : submitFutures){
							if(!future.isDone() && !future.isCancelled()){
								long cost = System.currentTimeMillis() - future.getStartTime(); 
								if(cost >= overTime * SECOND_UNIT){
									log.warn("cancle task[{}] which has time out, cost={}ms", future.getTaskId(), cost);
									future.cancel(true);
								}else{
									long leftTime = overTime - cost / SECOND_UNIT;
									if(leftTime < overTime){
										overTime = leftTime;
									}
								}
							}
						}
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // 保留中断请求，后面还要检测处理
			}
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private void runSchedul() throws Exception { 

			switchState(RUNNING);

			schedulTimes++;

			lastTime = System.currentTimeMillis();

			ScheduleBatch<E> schedulebatch = new ScheduleBatch<>(true, schedulTimes, lastTime);
			Task<E> task = null;
			try{
				Collection<? extends Task<E>> tasks = newSchedulTasks();
				if(!CollectionUtils.isEmpty(tasks)){
					synchronized (SUBMITMAP){
						for (Task<E> t : tasks) {
							task = t;
							task.scheduleBatch = schedulebatch;
							String taskId = task.getId();
							if (isTaskAlive(taskId)) {
								log.warn("task[{}] is still alive, create canceled.", taskId);
								continue;
							}

							TimedFuture future = submit(task);
							SUBMITMAP.put(taskId, future);    
							submitFutures.add(future);
						}
					}
				}
			} catch (RejectedExecutionException e) {
				Assert.notNull(task, "");
				log.warn("task[" + task.getId() + "] submit rejected.", e);
			}finally{ 
				schedulebatch.submitCompleted();
				checkScheduleComplete(schedulebatch);
				caculateNextTime(schedulebatch); 
			}
		}

		private void terminate(){
			config.pool.shutdown();

			if(waitShutDown()){
				try{
					onScheduleTerminate(schedulTimes, lastTime);
				}catch(Exception e){
					log.error("", e); 
				}
				switchState(STOPPED);
				isFirstRun = true;
			}

			cleanCompletedFutures();
		}

		private boolean waitShutDown(){
			boolean isStopping = false;
			while(true){
				if(!isStopping){
					synchronized (Context.this) {
						if(state == STOPPING){
							isStopping = true;
							config.pool.shutdownNow();
						}
					}
				}

				try {
					if(config.pool.awaitTermination(1, TimeUnit.DAYS)){
						log.info("context[{}] stoped.", name);
						return true;
					}else if(isStopping){
						log.warn("context[{}] is still stopping, though has waiting for a day.", name);
					}
				} catch (InterruptedException e) {
					log.warn("interrupt ignore when waiting task completetion."); 
				}
			}
		}
	}

	// 可能当前Context本次提交的task完成后，又有其它Context用同样的taskId提交了任务并正在执行，所以在真正从SUBMITMAP中删除时还需要在检测一下
	// 对SUBMITMAP的同步共用两处，一是任务提交时，二是这里，两处同步的动作都很小，开销可以接受
	// 使用submitFutures是为了在waitTaskCompleted中检测超时时不需要参与FUTUREMAP的同步
	private void cleanCompletedFutures(){ 
		synchronized (SUBMITMAP) {
			for(TimedFuture<Result<E>> currentFuture : submitFutures){
				String taskId = currentFuture.getTaskId();
				TimedFuture<Result<?>> otherFuture = SUBMITMAP.get(taskId);
				if(otherFuture.isDone()){ 
					SUBMITMAP.remove(taskId);
				}
			}
		}
		submitFutures.clear();
	}

	private boolean isTaskAlive(String taskId){
		Future<Result<?>> future = SUBMITMAP.get(taskId);
		return future != null && !future.isDone();
	}

	/**
	 * 提交批任务
	 * @param tasks
	 * @throws InterruptedException
	 */
	public void submitBatch(Collection<? extends Task<E>> tasks) {
		if(CollectionUtils.isEmpty(tasks)){
			return;
		}

		Iterator<? extends Task<E>> it = tasks.iterator();
		ScheduleBatch<E> scheduleBatch = new ScheduleBatch<>(false, batchSubmits.incrementAndGet(), System.currentTimeMillis());
		Task<E> task = null;
		try{
			while(it.hasNext()){
				task = it.next();
				task.scheduleBatch = scheduleBatch;
				submit(task);
			}
		} catch (RejectedExecutionException e) {
			Assert.notNull(task, "");
			log.warn("task[" + task.getId() + "] submit rejected.", e);
		}finally{ 
			scheduleBatch.submitCompleted();
			checkScheduleComplete(scheduleBatch);
		}
	}

	/**
	 * 提交任务
	 * @param task
	 * @return
	 */
	public TimedFuture<Result<E>> submit(Task<E> task) {
		if(SUBMITS.incrementAndGet() % SECOND_UNIT == 0){
			Monitor.jvm();
		}

		String taskId = task.getId();
		task.setContext(Context.this); 

		TimedFuture<Result<E>> future = config.pool.submit(task);
		if(task.scheduleBatch != null){
			task.scheduleBatch.increaseTaskNotCompleted();
		}
		log.info("task[{}] submitted.", taskId); 
		return future; 
	}

	@Override
	public Collection<? extends Task<E>> newSchedulTasks() throws Exception {
		Task<E> task = schedul();
		if(task != null){
			return Arrays.asList(task);
		}
		return null;
	}

	public Task<E> schedul() throws Exception {
		return null;
	}

	@Override
	public void onScheduleTerminate(long schedulTimes, long lastTime) {

	}

	void checkScheduleComplete(ScheduleBatch<E> scheduleBatch) {
		String s = scheduleBatch.isSchedul ? "schedul" : "submit";
		boolean isLastTaskComplete = scheduleBatch.hasTaskCompleted();
		boolean hasSubmitCompleted = scheduleBatch.hasSubmitCompleted();
		if(log.isDebugEnabled()){
			log.debug(s + "[" + scheduleBatch.getSchedulTimes() + "], isSubmitFinished：" 
					+ hasSubmitCompleted + ", taskNotCompleted：" + scheduleBatch.getTaskNotCompleted()); 
		}

		if(isLastTaskComplete && hasSubmitCompleted){ 
			if(log.isDebugEnabled()){
				String time = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(scheduleBatch.getSchedulTime());
				log.debug(scheduleBatch.getList().size() +  " tasks of " + s 
						+ "[" + scheduleBatch.getSchedulTimes() + "] submitted on " + time  + " completed.");
			}
			try{
				onScheduleComplete(scheduleBatch.getSchedulTimes(), scheduleBatch.getSchedulTime(), scheduleBatch.getList());
			}catch(Exception e){
				log.error("", e); 
			}
			scheduleBatch.taskCompleted();
		}
	}

	@Override
	public void onScheduleComplete(long batchTimes, long batchTime, List<Result<E>> results) throws Exception {

	}

	static class ScheduleBatch<E> {

		private final boolean isSchedul;

		private final long schedulTimes;

		private final long schedulTime;

		private final List<Result<E>> list = new ArrayList<>();

		private volatile boolean hasSubmitCompleted = false;

		private final AtomicInteger taskNotCompleted = new AtomicInteger(1);

		private final CountDownLatch completeLatch = new CountDownLatch(1);

		public void submitCompleted(){
			hasSubmitCompleted = true;
		}

		public boolean hasSubmitCompleted(){
			return hasSubmitCompleted;
		}

		public void taskCompleted(){
			completeLatch.countDown();
		} 

		public boolean waitTaskCompleted(long taskOverTime) throws InterruptedException{
			return completeLatch.await(taskOverTime, TimeUnit.SECONDS);
		}

		public ScheduleBatch(boolean isSchedul, long schedulTimes, long schedulTime){
			this.isSchedul = isSchedul;
			this.schedulTimes = schedulTimes;
			this.schedulTime = schedulTime;
		}

		public boolean isSchedul() {
			return isSchedul;
		}

		public long getSchedulTimes() {
			return schedulTimes;
		}

		public long getSchedulTime() {
			return schedulTime;
		}

		public synchronized void addResult(Result<E> result){
			list.add(result);
		}

		public synchronized List<Result<E>> getList() {
			return list;
		}

		public long increaseTaskNotCompleted(){
			return taskNotCompleted.incrementAndGet();
		}

		public boolean hasTaskCompleted(){
			return taskNotCompleted.decrementAndGet() == 0;
		}

		// 这里获取个近似值只是打日志用，不需要参与同步保证正确性
		public long getTaskNotCompleted(){
			return taskNotCompleted.get();
		}
	}

	// 记住class, 方便序列化
	String fom_context;

	String fom_schedul;
}
