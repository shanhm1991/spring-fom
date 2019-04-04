package com.fom.context;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fom.log.SlfLoggerFactory;

/**
 * 任务
 * <br>Callable的抽象实现，在context中作为最小执行单位，也可以单独创建调用或者提交线程池
 * 
 * @param <E> 任务结果数据类型
 * 
 * @author shanhm
 *
 */
public abstract class Task<E> implements Callable<Result<E>> {

	protected volatile Logger log = LoggerFactory.getLogger("ROOT");

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
	
	private volatile Context context;
	
	private volatile long createTime;
	
	private volatile long startTime;
	
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
	public final Result<E> call() {  
		Thread.currentThread().setName(id);
		Result<E> result = new Result<>(id); 
		long sTime = System.currentTimeMillis();
		this.startTime = sTime;
		result.startTime = sTime;
		result.createTime = this.createTime;
		
		log.info("task started."); 
		try {
			result.success = beforeExec();
			if(result.success){
				E content = exec();
				result.content = content;
				result.success = afterExec(content);
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
		if(resultHandler != null){
			try{
				resultHandler.handle(result);
			}catch(Exception e){
				log.error("", e); 
				result.success = false;
				if(result.throwable == null){
					result.throwable = e;
				}
			}
		}
		
		//这里算上resulthandler的结果和耗时
		long cost = System.currentTimeMillis() - sTime;
		if(result.success){
			if(context != null){
				context.statistics.successIncrease(id, cost, this.createTime, this.startTime); 
			}
			log.info("task success, cost={}ms", cost);
		}else{
			if(context != null){
				context.statistics.failedIncrease(id, result);
			}
			log.warn("task failed, cost={}ms", cost);
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
	
	final void setContext(Context context){
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
	protected final String getContextName(){
		if(context == null){
			return null;
		}
		return context.getName();
	}
	
	/**
	 * 只有在context中使用时才会赋值，否则将为null
	 * @return ContextConfig
	 */
	protected final ContextConfig getContextConfig(){
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
