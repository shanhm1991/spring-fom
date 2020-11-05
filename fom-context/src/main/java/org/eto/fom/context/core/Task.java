package org.eto.fom.context.core;

import java.util.concurrent.Callable;

import org.eto.fom.context.core.Context.ScheduleBatch;
import org.eto.fom.util.log.SlfLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务的抽象实现，在context中作为最小执行单位，也可以单独创建调用或者提交线程池
 * 
 * @param <E> 任务执行结果类型
 * 
 * @author shanhm
 *
 */
public abstract class Task<E> implements Callable<Result<E>> {

	protected volatile Logger log = LoggerFactory.getLogger("ROOT");

	public static final float FILE_UNIT = 1024.0f;

	public static final int SUCCESS_MIN = 200;

	public static final int SUCCESS_MAX = 207;

	/**
	 * 任务唯一标识
	 */
	protected final String id;

	/**
	 * 批任务执行状态，提交线程设置
	 */
	volatile ScheduleBatch<E> scheduleBatch;

	private volatile Context<E> context;

	private final long createTime;

	private volatile long startTime;

	/**
	 * @param id Task唯一标识
	 */
	public Task(String id) { 
		this.id = id;
		this.createTime = System.currentTimeMillis();
	}

	@Override
	public final Result<E> call() throws InterruptedException {   
		Thread.currentThread().setName(id);
		long sTime = System.currentTimeMillis();
		this.startTime = sTime;

		final Result<E> result = new Result<>(id); 
		result.startTime = sTime;
		result.createTime = this.createTime;

		if(log.isDebugEnabled()){
			log.debug("task started."); 
		}
		try {
			result.success = beforeExec();
			if(result.success){
				result.content = exec();
			}
		} catch(Throwable e) {
			log.error("", e); 
			result.success = false;
			result.throwable = e;
		} finally{
			try {
				afterExec(result.success, result.content, result.throwable);
			}catch(Throwable e) {
				log.error("", e); 
				result.success = false;
				result.throwable = e; // exec的throwable已经交给afterExec处理过，所以这里覆盖掉也能接受
			}
		}
		result.costTime = System.currentTimeMillis() - sTime;
		
		if(context != null){
			if(scheduleBatch != null){
				scheduleBatch.addResult(result); 
				context.checkScheduleComplete(scheduleBatch);
			}
			context.statistics.statistics(result); 
		}
		
		if(result.success){
			log.info("task success, cost={}ms {}", result.costTime, result.content);
		}else{
			log.warn("task failed, cost={}ms {}", result.costTime, result.content);
		}
		
		return result;
	}

	/**
	 * 任务执行前的工作
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected boolean beforeExec() throws Exception {
		return true;
	}

	/**
	 * 任务执行
	 * @return E
	 * @throws Exception Exception
	 */
	protected abstract E exec() throws Exception;

	/**
	 * 任务执行后的工作
	 * @param isExecSuccess exec是否成功
	 * @param content exec执行结果
	 * @param e exec抛出异常
	 * @throws Exception Exception
	 */
	protected void afterExec(boolean isExecSuccess,  E content, Throwable e) throws Exception {

	}

	/**
	 * 获取任务id
	 * @return id
	 */
	public final String getId() {
		return id;
	}

	/**
	 * 获取任务创建时间
	 * @return createTime
	 */
	public final long getCreateTime() {
		return createTime;
	}

	/**
	 * 获取任务开始时间
	 * @return startTime
	 */
	public final long getStartTime() {
		return startTime;
	}

	final void setContext(Context<E> context){
		if(context == null){
			return;
		}
		this.context = context;
		this.log = SlfLoggerFactory.getLogger(context.name); 
	}

	/**
	 * 只有在context中使用时才会赋值，否则将为null
	 * @return context name
	 */
	protected final String getName(){
		if(context == null){
			return null;
		}
		return context.getName();
	}

	/**
	 * 只有在context中使用时才会赋值，否则将为null
	 * @return ContextConfig
	 */
	protected final ContextConfig getConfig(){
		if(context == null){
			return null;
		}
		return context.config;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final boolean equals(Object obj) {
		if(!(obj instanceof Task)){
			return false;
		}
		Task<E> task = (Task<E>)obj;
		return this.id.equals(task.id);
	}

	@Override
	public final int hashCode() {
		return this.id.hashCode();
	}
}
