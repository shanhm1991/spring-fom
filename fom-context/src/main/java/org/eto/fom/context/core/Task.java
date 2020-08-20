package org.eto.fom.context.core;

import java.util.concurrent.Callable;

import org.eto.fom.context.core.Context.ScheduleBatch;
import org.eto.fom.util.log.SlfLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务
 * <br>Callable的抽象实现，在context中作为最小执行单位，也可以单独创建调用或者提交线程池
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
	 * 异常处理器
	 */
	protected ExceptionHandler exceptionHandler;

	/**
	 * 结果处理器
	 */
	protected ResultHandler<E> resultHandler;

	/**
	 * 批任务执行状态，提交线程设置
	 */
	volatile ScheduleBatch<E> scheduleBatch;

	private volatile Context<E> context;

	private volatile long createTime;

	private volatile long startTime;

	private boolean isResultHandler = false;

	/**
	 * @param id Task唯一标识
	 */
	public Task(String id) { 
		this.id = id;
		this.createTime = System.currentTimeMillis();
	}

	/**
	 * @param id 唯一标识
	 * @param exceptionHandler 异常处理器
	 */
	public Task(String id, ExceptionHandler exceptionHandler) { 
		this(id);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param id 唯一标识
	 * @param resultHandler 结果处理器
	 */
	public Task(String id, ResultHandler<E> resultHandler) { 
		this(id);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param id 唯一标识
	 * @param exceptionHandler 异常处理器
	 * @param resultHandler 结果处理器
	 */
	public Task(String id, ExceptionHandler exceptionHandler, ResultHandler<E> resultHandler) { 
		this(id);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	/**
	 * 设置异常处理器
	 * @param exceptionHandler exceptionHandler
	 */
	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * 结果处理器
	 * @param resultHandler resultHandler
	 */
	public void setResultHandler(ResultHandler<E> resultHandler) {
		this.resultHandler = resultHandler;
	}

	@Override
	public final Result<E> call() throws InterruptedException {   
		Thread.currentThread().setName(id);
		long sTime = System.currentTimeMillis();
		this.startTime = sTime;

		final Result<E> result = new Result<>(id); 
		result.startTime = sTime;
		result.createTime = this.createTime;

		log.info("task started."); 
		try {
			result.success = beforeExec();
			if(result.success){
				result.content = exec();
				result.success = afterExec(result.content);
			}
		} catch(Throwable e) {
			log.error("", e); 
			result.success = false;
			result.throwable = e;
			if(exceptionHandler != null){
				exceptionHandler.handle(e); 
			}
		}
		result.costTime = System.currentTimeMillis() - sTime;

		if(context != null){
			// ResultHandler也算在统计内
			if(result.success){
				context.statistics.successIncrease(id, result.costTime, this.createTime, this.startTime); 
				log.info("task success, cost={}ms", result.costTime);
			}else{
				context.statistics.failedIncrease(id, result);
				log.warn("task failed, cost={}ms", result.costTime);
			}

			if(!isResultHandler && scheduleBatch != null){
				scheduleBatch.addResult(result); 
				context.checkScheduleComplete(scheduleBatch);
			}

		}else{
			if(result.success){
				log.info("task success, cost={}ms", result.costTime);
			}else{
				log.warn("task failed, cost={}ms", result.costTime);
			}
		}

		if(!isResultHandler){
			try {
				handleResult(result); 
			} catch (Exception e) {
				log.error("", e);
			} 
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void handleResult(Result<E> result) throws Exception{
		if(resultHandler == null){
			return;
		}

		final Result<E> res = result.clone(); //result交接
		String tid = id + "-resultHandler";
		if(context == null){
			Thread t = new Thread(tid){
				@Override
				public void run() {
					try {
						resultHandler.handle(res);
					} catch (Exception e) {
						log.error("", e);
					}
				};
			};
			t.start();
		}else{
			Task task = new Task(tid){
				@Override
				protected Object exec() throws Exception {
					Task.this.resultHandler.handle(res);
					return null;
				}
			};
			task.isResultHandler = true;
			context.submit(task); 
		}
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
	 * @throws Exception
	 */
	protected abstract E exec() throws Exception;

	/**
	 * 任务执行后的工作
	 * @param e exec返回结果
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected boolean afterExec(E e) throws Exception {
		return true;
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
