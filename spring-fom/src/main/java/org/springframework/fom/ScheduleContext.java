package org.springframework.fom;

import static org.springframework.fom.State.INITED;
import static org.springframework.fom.State.RUNNING;
import static org.springframework.fom.State.SLEEPING;
import static org.springframework.fom.State.STOPPED;
import static org.springframework.fom.State.STOPPING;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.fom.interceptor.ScheduleCompleter;
import org.springframework.fom.interceptor.ScheduleFactory;
import org.springframework.fom.interceptor.ScheduleTerminator;
import org.springframework.fom.interceptor.TaskTimeoutHandler;
import org.springframework.fom.support.Response;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ScheduleContext<E> implements ScheduleFactory<E>, ScheduleCompleter<E>, ScheduleTerminator, TaskTimeoutHandler, ApplicationContextAware {

	private static final int SECOND_UNIT = 1000;

	// 所有schedule共用，以便根据id检测任务冲突
	private static Map<String,TimedFuture<Result<?>>> submitMap = new HashMap<>(512);

	// 当前提交的任务Future
	private List<TimedFuture<Result<E>>> submitFutures = new ArrayList<>();

	private final ScheduleConfig scheduleConfig = new ScheduleConfig();

	private final ScheduleStatistics scheduleStatistics = new ScheduleStatistics();

	// schedule加载时间
	private final long loadTime = System.currentTimeMillis();

	private String scheduleName;

	private String scheduleBeanName;

	protected Logger logger;

	// 最近一次执行时间
	private volatile long lastTime;

	// 下次执行时间
	private volatile long nextTime;

	// 执行次数
	private volatile long schedulTimes;

	// 是否是启动后的第一次执行
	private boolean isFirstRun = true;

	private State state = INITED; 

	private ApplicationContext applicationContext;
	
	// running 状态纪录exec请求
	private final AtomicBoolean nextTimeHasSated = new AtomicBoolean(false);

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ScheduleInfo getScheduleInfo(){
		return new ScheduleInfo(this);
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}

	public void setScheduleBeanName(String scheduleBeanName) {
		this.scheduleBeanName = scheduleBeanName;
	}

	public String getScheduleBeanName() {
		return scheduleBeanName;
	}

	public long getLoadTime() {
		return loadTime;
	}

	public long getLastTime() {
		return lastTime;
	}

	public long getNextTime() {
		return nextTime;
	}

	public long getSchedulTimes() {
		return schedulTimes;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public ScheduleConfig getScheduleConfig() {
		return scheduleConfig;
	}

	public ScheduleStatistics getScheduleStatistics() {
		return scheduleStatistics;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onScheduleTerminate(long execTimes, long lastExecTime) {
		// 这里要从原对象方法调用到代理对象中的方法
		ScheduleContext<E> scheduleContext;
		if(applicationContext != null && scheduleBeanName != null
				&& (scheduleContext = (ScheduleContext<E>)applicationContext.getBean(scheduleName)) != null){
			scheduleContext.onScheduleTerminate(execTimes, lastExecTime); 
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onScheduleComplete(long execTimes, long lastExecTime, List<Result<E>> results) throws Exception {
		ScheduleContext<E> scheduleContext;
		if(applicationContext != null && scheduleBeanName != null 
				&& (scheduleContext = (ScheduleContext<E>)applicationContext.getBean(scheduleName)) != null){
			scheduleContext.onScheduleComplete(execTimes, lastExecTime, results);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleTimeout(long costTime) {
		ScheduleContext<E> scheduleContext;
		if(applicationContext != null && scheduleBeanName != null 
				&& (scheduleContext = (ScheduleContext<E>)applicationContext.getBean(scheduleName)) != null){
			scheduleContext.handleTimeout(costTime);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<? extends Task<E>> newSchedulTasks() throws Exception {
		ScheduleContext<E> scheduleContext;
		if(applicationContext != null 
				&& (scheduleContext = (ScheduleContext<E>)applicationContext.getBean(scheduleName)) != null){
			return scheduleContext.newSchedulTasks();
		}

		Task<E> task = schedul();
		if(task != null){
			return Arrays.asList(task);
		}
		return new ArrayList<>();
	}

	public Task<E> schedul() throws Exception {
		return null;
	}

	private synchronized void switchState(State state) {
		this.state = state; 
	}

	public synchronized State getState(){
		if(state == STOPPING && scheduleConfig.getPool().isTerminated()){
			state = STOPPED;
			isFirstRun = true;
		}
		return state;
	}

	public Response<Void> scheduleStart(){
		synchronized (this) {
			switch(state){
			case INITED:
			case STOPPED:
				if(scheduleConfig.getPool().isTerminated()){
					scheduleConfig.reset();
				}
				scheduleThread = new ScheduleThread();
				scheduleThread.start();
				logger.info("schedule[{}] startup", scheduleName); 
				return new Response<>(Response.SUCCESS, "schedule[" + scheduleName + "] startup.");
			case STOPPING:
				logger.warn("schedule[{}] is stopping, cann't startup.", scheduleName); 
				return new Response<>(Response.FAILED, "schedule[" + scheduleName + "] is stopping, cann't startup.");
			case RUNNING:
			case SLEEPING:
				logger.warn("schedule[{}] was already startup.", scheduleName); 
				return new Response<>(Response.FAILED, "schedule[" + scheduleName + "] was already startup.");
			default:
				return new Response<>(Response.FAILED, "schedule state invalid.");
			}
		}
	}

	public Response<Void> scheduleShutdown(){
		synchronized (this) {
			switch(state){
			case INITED:
				logger.warn("schedule[{}] was not startup.", scheduleName); 
				return new Response<>(Response.FAILED, "schedule[" + scheduleName + "] was not startup."); 
			case STOPPED:
				logger.warn("schedule[{}] was already stopped.", scheduleName); 
				return new Response<>(Response.FAILED, "schedule[" + scheduleName + "] was already stopped.");
			case STOPPING:
				logger.warn("schedule[{}] is stopping, cann't stop.", scheduleName); 
				return new Response<>(Response.FAILED, "schedule[" + scheduleName + "] is already stopping.");
			case RUNNING:
			case SLEEPING:
				state = STOPPING;
				scheduleThread.interrupt(); //尽快响应
				if(scheduleConfig.getPool().isTerminated()){
					state = STOPPED;
					isFirstRun = true;
				}
				logger.info("schedule[{}] will stop soon.", scheduleName);
				return new Response<>(Response.SUCCESS, "schedule[" + scheduleName + "] will stop soon.");
			default:
				return new Response<>(Response.FAILED, "schedule state invalid.");
			}
		}
	}

	public Response<Void> scheduleExecNow(){
		synchronized (this) {
			switch(state){
			case INITED:
			case STOPPED:
				logger.warn("schedule[{}] was not startup, cann't execut now.", scheduleName); 
				return new Response<>(Response.FAILED, "schedule[" + scheduleName + "] was not startup, cann't execut now.");
			case STOPPING:
				logger.warn("schedule[{}] is stopping, cann't execut now.", scheduleName); 
				return new Response<>(Response.FAILED, "schedule[" + scheduleName + "] is stopping, cann't execut now.");
			case RUNNING:
				if(scheduleConfig.getIgnoreExecRequestWhenRunning()){
					logger.warn("schedule[{}] is already running.", scheduleName); 
					return new Response<>(Response.FAILED, "schedule[" + scheduleName + "] is already running.");
				}else{
					if(!nextTimeHasSated.compareAndSet(false, true)){
						logger.info("schedule[{}] is running, and exec was already requested.", scheduleName); 
						return new Response<>(Response.FAILED, "schedule[" + scheduleName + "] is running, and exec was already requested.");
					}
					nextTime = System.currentTimeMillis();
					logger.info("schedule[{}] is running, and will re-exec immediately after completion.", scheduleName); 
					return new Response<>(Response.SUCCESS, "schedule[" + scheduleName + "]  is running, and will re-exec immediately after completion .");
				}
			case SLEEPING:
				scheduleThread.interrupt();
				logger.info("schedule[{}] execute now.", scheduleName); 
				return new Response<>(Response.SUCCESS, "schedule[" + scheduleName + "] execute now.");
			default:
				return new Response<>(Response.FAILED, "schedule state invalid.");
			}
		}
	}

	private ScheduleThread scheduleThread;

	private class ScheduleThread extends Thread {

		public ScheduleThread(){
			this.setName("schedule[" + scheduleName + "]"); 
		}

		@Override
		public void run() {
			while(true){
				boolean isStopping;
				synchronized (ScheduleContext.this) {
					isStopping = state == STOPPING;
				}
				if(isStopping){
					terminate();
					return;
				}

				try{
					if(isFirstRun && !scheduleConfig.getExecOnLoad()){
						isFirstRun = false;
						caculateNextTime(null); 
					}else{
						runSchedul();
					}
				}catch(InterruptedException e){
					// ignore 进入下次循环，获取判断当前状态
				}catch (RuntimeException e){
					logger.error("", e);
				}catch(Throwable e){
					logger.error("schedule terminated unexpectedly", e); 
					terminate();
					return;
				}

				if(nextTime > 0) {
					long waitTime = nextTime - System.currentTimeMillis();
					if(!Thread.interrupted() && waitTime > 0){ 
						switchState(SLEEPING);
						synchronized(this) {
							try {
								wait(waitTime);
							} catch (InterruptedException e) {
								// 中断即重新执行
							}
						}
					}
				}else{
					synchronized(ScheduleContext.this){
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

			if(scheduleConfig.getCron() != null){
				if(!nextTimeHasSated.compareAndSet(true, false)){ 
					nextTime = scheduleConfig.getCron().getTimeAfter(last).getTime();
				}
				waitTaskCompleted(scheduleBatch);
			}else if(scheduleConfig.getFixedRate() > 0){
				if(!nextTimeHasSated.compareAndSet(true, false)){ 
					nextTime = last.getTime() + scheduleConfig.getFixedRate() * SECOND_UNIT;
				}
				waitTaskCompleted(scheduleBatch);
			}else if(scheduleConfig.getFixedDelay() > 0){
				waitTaskCompleted(scheduleBatch);
				if(!nextTimeHasSated.compareAndSet(true, false)){ 
					nextTime = System.currentTimeMillis() + scheduleConfig.getFixedDelay() * SECOND_UNIT;
				}
			}else{
				waitTaskCompleted(scheduleBatch);
				nextTime = 0;
			}
		}

		private void waitTaskCompleted(ScheduleBatch<E> scheduleBatch){
			if(scheduleBatch == null){
				return;
			}
			
			long overTime = scheduleConfig.getTaskOverTime();
			boolean detectTimeoutOnEachTask = scheduleConfig.getDetectTimeoutOnEachTask();
			boolean cancelTaskOnTimeout = scheduleConfig.getCancelTaskOnTimeout();
			try {
				if(FomSchedule.TASK_OVERTIME_DEFAULT == overTime){ // 默认不检测超时
					scheduleBatch.waitTaskCompleted();
					cleanCompletedFutures();
				}else{
					if(!detectTimeoutOnEachTask){ // 对整体任务算超时
						if(!scheduleBatch.waitTaskCompleted(overTime)){ 
							for(TimedFuture<Result<E>> future : submitFutures){
								if(!future.isDone()){
									long startTime = future.getStartTime();  
									if(startTime > 0){
										long cost = System.currentTimeMillis() - future.getStartTime(); 
										if(cost >= overTime * SECOND_UNIT){
											try{
												handleTimeout(cost);
											}catch(Exception e){
												logger.error("", e); 
											}
										}
										
										if(cancelTaskOnTimeout){ 
											logger.warn("cancle task[{}] due to time out, cost={}ms", future.getTaskId(), cost);
											future.cancel(true);
										}
									}else{
										logger.warn("cancle task[{}] which has not started, cost={}ms", future.getTaskId(), 0);
										future.cancel(true);
									}
								}
							}
						}
						
						scheduleBatch.waitTaskCompleted();
						cleanCompletedFutures();
						return;
					}else{ // 对每个任务单独检测超时
						long checkTime = overTime;
						logger.error("checkTime" + checkTime); 
						while(true){
							if(scheduleBatch.waitTaskCompleted(checkTime)){ 
								cleanCompletedFutures();
								return;
							}else{
								Set<String> needNotWait = new HashSet<>(); 
								boolean checkTimeReset = false;
								for(TimedFuture<Result<E>> future : submitFutures){
									if(!future.isDone()){
										long startTime = future.getStartTime();  
										if(startTime > 0){ // startTime = 0 表示任务还没启动
											long cost = System.currentTimeMillis() - startTime; 
											if(cost >= checkTime * SECOND_UNIT){ 
												if(!needNotWait.add(future.getTaskId())){
													continue;
												}
												
												try{
													handleTimeout(cost);
												}catch(Exception e){
													logger.error("", e); 
												}
												
												if(cancelTaskOnTimeout){ 
													logger.warn("cancle task[{}] due to time out, cost={}ms", future.getTaskId(), cost);
													future.cancel(true);
												}
											}else{
												// 获取一个最近要超时的任务时间
												long leftTime = checkTime - cost / SECOND_UNIT;
												if(leftTime < checkTime){
													checkTime = leftTime;
													checkTimeReset = true;
												}
											}
										}
									}else{
										needNotWait.add(future.getTaskId());
									}
								}
								
								// 如果检查一轮之后，checkTime没有被重置，则恢复成overTime
								if(!checkTimeReset && checkTime != overTime){ 
									checkTime = overTime;
								}
								
								// 已经完成的，或者已经执行过handleTimeout的任务不需要再等
								if(needNotWait.size() == submitFutures.size()){
									scheduleBatch.waitTaskCompleted();
									cleanCompletedFutures();
									return;
								}
							}
						}
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // 保留中断请求，后面检测处理
			}
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private void runSchedul() throws Exception { 

			switchState(RUNNING);

			schedulTimes++;

			lastTime = System.currentTimeMillis();

			ScheduleBatch<E> scheduleBatch = new ScheduleBatch<>(true, schedulTimes, lastTime);
			Task<E> task = null;
			try{
				Collection<? extends Task<E>> tasks = newSchedulTasks();
				if(tasks != null && !tasks.isEmpty()){
					synchronized(submitMap){
						for (Task<E> t : tasks) {
							task = t;
							String taskId = task.getTaskId();
							task.setScheduleBatch(scheduleBatch);
							if (isTaskAlive(taskId)) {
								logger.warn("task[{}] is still alive, create canceled.", taskId);
								continue;
							}

							TimedFuture future = submit(task);
							submitMap.put(taskId, future);    
							submitFutures.add(future);
						}
					}
				}
			} catch (RejectedExecutionException e) {
				Assert.notNull(task, "");
				logger.warn("task[{}] submit rejected.", task.getTaskId(), e); 
			}finally{ 
				scheduleBatch.submitCompleted();
				checkScheduleComplete(scheduleBatch);
				caculateNextTime(scheduleBatch); 
			}
		}

		private void terminate(){
			scheduleConfig.getPool().shutdown();
			if(waitShutDown()){
				try{
					onScheduleTerminate(schedulTimes, lastTime);
				}catch(Exception e){
					logger.error("", e); 
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
					synchronized (ScheduleContext.this) {
						if(state == STOPPING){
							isStopping = true;
							scheduleConfig.getPool().shutdownNow();
						}
					}
				}

				try {
					if(scheduleConfig.getPool().awaitTermination(1, TimeUnit.DAYS)){
						logger.info("schedule[{}] stoped.", scheduleName);
						return true;
					}else if(isStopping){
						logger.warn("schedule[{}] is still stopping, though has waiting for a day.", scheduleName);
					}
				} catch (InterruptedException e) {
					logger.warn("interrupt ignore when waiting task completetion."); 
				}
			}
		}
	}

	public TimedFuture<Result<E>> submit(Task<E> task) {
		task.setScheduleContext(ScheduleContext.this); 

		TimedFuture<Result<E>> future = scheduleConfig.getPool().submit(task);
		if(task.getScheduleBatch() != null){
			task.getScheduleBatch().increaseTaskNotCompleted();
		}
		logger.info("task[{}] submitted.", task.getTaskId()); 
		return future; 
	}

	private boolean isTaskAlive(String taskId){
		Future<Result<?>> future = submitMap.get(taskId);
		return future != null && !future.isDone();
	}

	void checkScheduleComplete(ScheduleBatch<E> scheduleBatch) {
		String s = scheduleBatch.isSchedul ? "schedul" : "submit";
		boolean isLastTaskComplete = scheduleBatch.hasTaskCompleted();
		boolean hasSubmitCompleted = scheduleBatch.hasSubmitCompleted();
		logger.debug(s + "[" + scheduleBatch.getSchedulTimes() + "], isSubmitFinished：" 
				+ hasSubmitCompleted + ", taskNotCompleted：" + scheduleBatch.getTaskNotCompleted()); 

		if(isLastTaskComplete && hasSubmitCompleted){ 
			String time = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(scheduleBatch.getSchedulTime());
			logger.debug(scheduleBatch.getList().size() +  " tasks of " + s 
					+ "[" + scheduleBatch.getSchedulTimes() + "] submitted on " + time  + " completed.");
			try{
				onScheduleComplete(scheduleBatch.getSchedulTimes(), scheduleBatch.getSchedulTime(), scheduleBatch.getList());
			}catch(Exception e){
				logger.error("", e); 
			}
			scheduleBatch.taskCompleted();
		}
	}

	// 可能当前schedule提交的task完成后，又有其它schedule用同样taskId提交任务并正在执行，所以在真正从submitMap中删除时还要再检测一下
	// 使用submitFutures是为了在waitTaskCompleted中检测超时不需要参与submitMap的同步
	private void cleanCompletedFutures(){ 
		synchronized(submitMap) {
			for(TimedFuture<Result<E>> currentFuture : submitFutures){
				String taskId = currentFuture.getTaskId();
				TimedFuture<Result<?>> future = submitMap.get(taskId);
				if(future != null && future.isDone()){ 
					submitMap.remove(taskId);
				}
			}
		}
		submitFutures.clear();
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
 
		public void waitTaskCompleted() throws InterruptedException{
			completeLatch.await();
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

		public long getTaskNotCompleted(){
			return taskNotCompleted.get();
		}
	}

	public Map<String, String> getWaitingTasks(){
		return scheduleConfig.getWaitingTasks();
	}

	public List<Map<String, String>> getActiveTasks(){
		return scheduleConfig.getActiveTasks();
	}

	public Map<String, Object> getSuccessStat(String statDay) throws ParseException {  
		return scheduleStatistics.getSuccessStat(statDay);
	}

	public List<Map<String, String>> getFailedStat() {
		return scheduleStatistics.getFailedStat();
	}

	public void saveConfig(HashMap<String, Object> map, boolean valueEnvirment) throws NumberFormatException, IllegalArgumentException, IllegalAccessException{
		Set<Field> envirmentChange = scheduleConfig.saveConfig(map);
		if(!valueEnvirment || envirmentChange.isEmpty()){
			return;
		}
		valueEnvirmentField(envirmentChange);
	} 

	void valueEnvirmentField(Set<Field> envirmentChange) throws NumberFormatException, IllegalArgumentException, IllegalAccessException{
		// 修改当前schedul引用对应变量属性值，不保证线程安全 
		for(Field field: envirmentChange){
			Value value = field.getAnnotation(Value.class);
			String expression = value.value();

			List<String> list = FomBeanPostProcessor.getProperties(expression);
			for(String ex : list){
				int index = ex.indexOf(":");
				if(index == -1){
					index = ex.indexOf("}");
				}
				String key = ex.substring(2, index);

				String confValue = scheduleConfig.get(key);
				expression = expression.replace(ex, confValue);
			}

			ReflectionUtils.makeAccessible(field);
			Object instance = this;
			if(scheduleBeanName != null && applicationContext != null){
				instance = applicationContext.getBean(scheduleBeanName);
			}

			switch(field.getGenericType().toString()){
			case "short":
			case "class java.lang.Short":
				field.set(instance, Short.valueOf(expression));
				break;
			case "int":
			case "class java.lang.Integer":
				field.set(instance, Integer.valueOf(expression));
				break;
			case "long":
			case "class java.lang.Long":
				field.set(instance, Long.valueOf(expression));
				break;
			case "float":
			case "class java.lang.Float":
				field.set(instance, Float.valueOf(expression));
				break;
			case "double":
			case "class java.lang.Double":
				field.set(instance, Double.valueOf(expression));
				break;
			case "boolean":
			case "class java.lang.Boolean":
				field.set(instance, Boolean.valueOf(expression));
				break;
			case "class java.lang.String":
				field.set(instance, expression);
				break;
			default:
				throw new UnsupportedOperationException("value set failed：" + instance.getClass().getName() + "." + field.getName());
			}
		}
	}

}
