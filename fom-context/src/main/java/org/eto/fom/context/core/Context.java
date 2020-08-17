package org.eto.fom.context.core;

import static org.eto.fom.context.core.State.INITED;
import static org.eto.fom.context.core.State.RUNNING;
import static org.eto.fom.context.core.State.SLEEPING;
import static org.eto.fom.context.core.State.STOPPED;
import static org.eto.fom.context.core.State.STOPPING;
import static org.eto.fom.context.core.State.WAITING;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.eto.fom.context.Monitor;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.util.log.SlfLoggerFactory;
import org.slf4j.Logger;

import com.google.gson.annotations.Expose;

/**
 * 模块最小单位，相当于一个组织者的角色，负责创建和组织Task的运行
 * 
 * @author shanhm
 * 
 * @param <E> 任务执行结果类型
 *
 */
public class Context<E> {

	private static final long UNIT = 1000;

	/**
	 * 所有的Context共用，防止两个Context创建针对同一个文件的任务
	 */
	static final Map<String,TimedFuture<Result<?>>> FUTUREMAP = new ConcurrentHashMap<>(1000);

	/**
	 * 当前构造的context的名称
	 */
	static final ThreadLocal<String> localName = new ThreadLocal<>();

	/**
	 * 加载时间点
	 */
	@Expose
	final long loadTime = System.currentTimeMillis();

	/**
	 * 统计信息
	 */
	@Expose
	final ContextStatistics statistics = new ContextStatistics();

	/**
	 * 名称，不可变
	 */
	protected final String name;

	/**
	 * 配置池
	 */
	protected final ContextConfig config;

	@Expose
	protected Logger log;

	/**
	 * 上次执行时间点
	 */
	@Expose
	volatile long lastTime;

	/**
	 * 下次执行时间点
	 */
	@Expose
	volatile long nextTime;

	/**
	 * 周期执行次数
	 */
	@Expose
	volatile long batchScheduls;

	/**
	 * 是否是在启动时执行
	 */
	@Expose
	private boolean runOnStartup = true;

	/**
	 * 状态
	 */
	@Expose
	private State state = INITED; 

	/**
	 * 所有的任务提交总数数，每当次数达到1000时执行一次cleanFutures，定时线程和submit线程共享
	 */
	@Expose
	private AtomicLong submits = new AtomicLong(); 

	/**
	 * 批任务提交次数
	 */
	@Expose
	private AtomicLong batchSubmits = new AtomicLong(); 

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
	 * 注册到容器
	 * @param loadFrom
	 * @throws Exception
	 */
	void regist(boolean putConfig) throws Exception {
		ContextManager.register(this, putConfig);  
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
	public final Map<Task<?>, Thread> getActiveThreads() {
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
		return logger.getLevel().toString();
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
				runOnStartup = true;
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
			case WAITING:
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
			case WAITING:
			case SLEEPING:
				state = STOPPING;
				if(innerThread.isAlive()){
					innerThread.interrupt();//尽快响应
					map.put("result", true);
					map.put("msg", "context[" + name + "] is stopping.");
					log.info("context[{}] is stopping.", name); 
					return map;
				}else{
					state = STOPPED;
					runOnStartup = true;
					map.put("result", true);
					map.put("msg", "context[" + name + "] stopped.");
					log.info("context[{}] stopped.", name); 
					return map;
				}
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
			case WAITING:
				map.put("result", false);
				map.put("msg", "context[" + name + "] is waiting for task completion.");
				log.info("context[{}] is waiting for task completion.", name); 
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
				synchronized (Context.this) {
					isStopping = state == STOPPING;
				}
				if(isStopping){
					stopSelf();
					return;
				}

				if(runOnStartup && !config.getExecOnLoad()){
					runOnStartup = false;
				}else{
					try {
						runSchedul();
					} catch (RuntimeException e){
						log.error("", e);
					}catch(Throwable e){
						log.error("context[" + name + "] terminated unexpectedly", e); 
						stopSelf();
						return;
					}
				}

				if(config.cronExpression != null) {
					Date last = new Date();
					if(lastTime > 0){
						last = new Date(lastTime);
					}

					Date next = config.cronExpression.getTimeAfter(last);
					nextTime = next.getTime();

					long waitTime = nextTime - System.currentTimeMillis();
					if(waitTime > 0){ //考虑到runSchedul()本身的耗时
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

					if(config.getStopWithNoCron()){
						terminate();
						return;
					}else{
						switchState(SLEEPING);
						synchronized (this) {
							try {
								wait();
							} catch (InterruptedException e) {
								//借助interrupted标记来中断睡眠，立即重新执行
							}
						}
					}
				}
			}
		}

		private void runSchedul() throws Exception { 
			cleanFutures();
			
			switchState(RUNNING);
			lastTime = System.currentTimeMillis();
			batchScheduls++;
			Collection<? extends Task<E>> tasks = scheduleBatch();
			if(!CollectionUtils.isEmpty(tasks)){
				Iterator<? extends Task<E>> it = tasks.iterator();
				BatchStatus<E> batchStatus = new BatchStatus<>(true, batchScheduls, lastTime);
				Task<E> task = null;
				try{
					while(it.hasNext()){
						task = it.next();
						task.batchStatus = batchStatus;
						String taskId = task.getId();
						if(isTaskAlive(taskId)){ 
							log.warn("task[{}] is still alive, create canceled.", taskId); 
							continue;
						}
						submit(task);
					}
				} catch (RejectedExecutionException e) {
					log.warn("task[" + task.getId() + "] submit rejected.", e);
				}finally{ 
					batchStatus.countDown();
					checkBatchComplete(batchStatus);
				}
			}
		}

		private void terminate(){
			switchState(WAITING);
			config.pool.shutdown();

			if(waitTask()){
				onScheduleTerminate();
				switchState(STOPPED);
				runOnStartup = true;
			}

			cleanFutures();
		}

		private boolean waitTask(){
			boolean isStopping = false;
			while(true){
				try {
					if(config.pool.awaitTermination(1, TimeUnit.DAYS)){
						log.info("context[{}] stoped.", name);
						return true;
					}else if(isStopping){
						log.warn("context[{}] is still stopping, though has waiting for a day.", name);
						return false;
					}
				} catch (InterruptedException e) {
					log.warn("interrupted when waiting executing task."); 
				}

				synchronized (Context.this) {
					if(state == STOPPING){
						isStopping = true;
						config.pool.shutdownNow();
					}
				}
			}
		}

		private void stopSelf(){
			config.pool.shutdownNow();
			boolean stopSucc = false;
			try {
				stopSucc = config.pool.awaitTermination(1, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				log.error("interrupted when stopping, which should never happened."); 
			}
			cleanFutures();
			synchronized (Context.this) {
				if(stopSucc){
					state = STOPPED;
					runOnStartup = true;
					log.info("context[{}] stoped.", name);
				}else{
					log.warn("context[{}] is still stopping, though has waiting for a day.", name);
				}
			}
		}
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

	/**
	 * 提交批任务
	 * @param tasks
	 * @throws InterruptedException 
	 * @throws Exception
	 */
	public void submitBatch(Collection<? extends Task<E>> tasks) throws InterruptedException  {
		if(CollectionUtils.isEmpty(tasks)){
			return;
		}

		Iterator<? extends Task<E>> it = tasks.iterator();
		BatchStatus<E> batchStatus = new BatchStatus<>(false, batchSubmits.incrementAndGet(), System.currentTimeMillis());
		Task<E> task = null;
		try{
			while(it.hasNext()){
				task = it.next();
				task.batchStatus = batchStatus;
				String taskId = task.getId();
				if(isTaskAlive(taskId)){ 
					log.warn("task[{}] is still alive, create canceled.", taskId); 
					continue;
				}
				submit(task);
			}
		} catch (RejectedExecutionException e) {
			log.warn("task[" + task.getId() + "] submit rejected.", e);
		}finally{ 
			batchStatus.countDown();
			checkBatchComplete(batchStatus);
		}
	}

	/**
	 * 提交任务
	 * @param task task
	 * @return TimedFuture
	 * @throws Exception Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TimedFuture<Result<E>> submit(Task<E> task) {
		if(submits.incrementAndGet() % UNIT == 0){
			Monitor.jvm();
			cleanFutures(); 
		}
		
		String taskId = task.getId();
		task.setContext(Context.this); 

		TimedFuture future = config.pool.submit(task);
		if(task.batchStatus != null){
			task.batchStatus.increaseTaskNotCompleted();
		}

		FUTUREMAP.put(taskId, future); 
		log.info("task[{}] created.", taskId); 
		return future; 
	}
	
	/**
	 * 周期执行任务
	 * @return
	 * @throws Exception
	 */
	protected Task<E> schedule() throws Exception {
		return null;
	}

	/**
	 * 周期执行批量任务
	 * @return task set
	 * @throws Exception Exception
	 */
	protected Collection<? extends Task<E>> scheduleBatch() throws Exception {
		List<Task<E>> list = new LinkedList<>();
		Task<E> task = schedule();
		if(task != null){
			list.add(task);
		}
		return list;
	}

	/**
	 * stopWithNoCron = true，结束时触发
	 */
	protected void onScheduleTerminate() {

	}

	void checkBatchComplete(BatchStatus<E> batchStatus) throws InterruptedException{
		String s = batchStatus.isSchedul ? "schedul" : "submit";
		boolean isLastTaskComplete = batchStatus.isLastTaskComplete();
		boolean hasCountDown = batchStatus.hasCountDown();
		if(log.isDebugEnabled()){
			log.debug(s + "[" + batchStatus.getBatch() + "], isSubmitFinished：" 
					+ hasCountDown + ", taskNotCompleted：" + batchStatus.getTaskNotCompleted()); 
		}
		
		if(isLastTaskComplete && hasCountDown){ 
			if(log.isDebugEnabled()){
				String time = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(batchStatus.getBatchTime());
				log.debug(batchStatus.getList().size() +  " tasks of " + s 
						+ "[" + batchStatus.getBatch() + "] created on " + time  + " completed.");
			}
			onBatchComplete(batchStatus.getBatch(), batchStatus.getBatchTime(), batchStatus.getList());
		}
		
	}

	/**
	 * 周期性提交的任务都执行完时触发
	 * @param batch 批次
	 * @param batchTime 周期执行的时间点 
	 * @param results 任务执行的结果集
	 */
	protected void onBatchComplete(long batch, long batchTime, List<Result<E>> results) {

	}

	/**
	 * 检测时机：
	 * <p>1.定时任务每次开始时
	 * <p>2.定时模块关闭(shutdown)时，或者自行结束时
	 * <p>3.每当提交过的任务数达到1000的整数倍时（考虑到主动submit任务的常见）
	 */
	private void cleanFutures(){
		new Thread(name + "-clean"){
			@Override
			public void run() {
				Iterator<Map.Entry<String, TimedFuture<Result<?>>>> it = FUTUREMAP.entrySet().iterator();
				while(it.hasNext()){
					Entry<String, TimedFuture<Result<?>>> entry = it.next();
					TimedFuture<Result<?>> future = entry.getValue();
					//只检测自己提交的任务
					if(name.equals(future.getContextName())){   
						String taskId = entry.getKey(); 
						if(future.getStartTime() > 0 && !future.isDone()){
							long existTime = (System.currentTimeMillis() - future.getStartTime()) / UNIT;
							int threadOverTime = Integer.parseInt(config.get(ContextConfig.CONF_OVERTIME)); 
							if(existTime > threadOverTime) {
								log.warn("task overtime[{}], {}s", taskId, existTime);
								if(Boolean.parseBoolean(config.get(ContextConfig.CONF_CANCELLABLE))) { 
									future.cancel(true);
								}
							}
							continue;
						}
						it.remove();
					}
				}
			}
		}.start();

	}

	/**
	 * 定义BatchStatus是为了实现onBatchComplete，
	 * 由于submit有一定步骤（任务执行非常快），要防止出现任务还在提交中，但是已提交的任务已经执行完，导致提前触发onBatchComplete
	 * 上面的问题可以通过submitLatch控制，确保onBatchComplete一定在任务提交完成之后触发；
	 * 但是还这样又可能出现全部任务已经执行完，但是submitLatch还没有countDown，导致onBatchComplete不被触发；
	 * 所以需要taskNotCompleted提前置1，以便在全部提交完成时主动检查是否触发onBatchComplete
	 */
	static class BatchStatus<E> {

		private final boolean isSchedul;

		private final long batch;

		private final long batchTime;

		private final List<Result<E>> list = new ArrayList<>();

		private final CountDownLatch submitLatch = new CountDownLatch(1);

		private final AtomicLong taskNotCompleted = new AtomicLong(1);

		public void countDown(){
			submitLatch.countDown();
		}

		public boolean hasCountDown() throws InterruptedException{
			return submitLatch.await(0, TimeUnit.MILLISECONDS);
		}

		public BatchStatus(boolean isSchedul, long batch, long batchTime){
			this.isSchedul = isSchedul;
			this.batch = batch;
			this.batchTime = batchTime;
		}

		public boolean isSchedul() {
			return isSchedul;
		}

		public long getBatch() {
			return batch;
		}

		public long getBatchTime() {
			return batchTime;
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

		public boolean isLastTaskComplete(){
			return taskNotCompleted.decrementAndGet() == 0;
		}

		// 这里获取个近似值只是打日志用，不需要参与同步保证正确性
		public long getTaskNotCompleted(){
			return taskNotCompleted.get();
		}
	}

	// 记住class,反序列化时使用
	String fom_context;

	String fom_schedul;

	String fom_schedulbatch;

}
