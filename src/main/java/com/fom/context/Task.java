package com.fom.context;

import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 任务
 * <br>Callable的抽象实现，在context中作为最小执行单位，也可以单独创建调用或者提交线程池
 * 
 * @author shanhm
 *
 */
public abstract class Task implements Callable<Result> {

	protected volatile Logger log = Logger.getRootLogger();

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
	protected ResultHandler resultHandler;
	
	/**
	 * 只有在context中使用时才会赋值，否则将为null，
	 */
	protected volatile String contextName;

	/**
	 * @param id 创建Executor的资源
	 */
	public Task(String id) { 
		this.id = id;
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
	public Task(String id, ResultHandler resultHandler) { 
		this(id);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param id 唯一标识
	 * @param exceptionHandler 异常处理器
	 * @param resultHandler 结果处理器
	 */
	public Task(String id, ExceptionHandler exceptionHandler, ResultHandler resultHandler) { 
		this(id);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	public final Result call() throws Exception {  
		Thread.currentThread().setName(id);
		Result result = new Result(id); 
		long sTime = System.currentTimeMillis();
		result.startTime = sTime;
		try {
			boolean res = beforeExec() && exec() && afterExec();
			long cost = System.currentTimeMillis() - sTime;
			if(res){
				log.info("task finished, cost=" + cost + "ms");
			}else{
				log.warn("task failed, cost=" + cost + "ms");
			}
			result.success = res;
			result.costTime = cost;
		} catch(Throwable e) {
			long cost = System.currentTimeMillis() - sTime;
			log.error("task failed, cost=" + cost, e);
			result.success = false;
			result.throwable = e;
			if(exceptionHandler != null){
				exceptionHandler.handle(e); 
			}
		}
		
		if(resultHandler != null){
			resultHandler.handle(result);
		}
		return result;
	}

	/**
	 * 任务执行
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected abstract boolean exec() throws Exception;

	/**
	 * 任务执行前的工作
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected boolean beforeExec() throws Exception {
		return true;
	}
	
	/**
	 * 任务执行后的工作
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected boolean afterExec() throws Exception {
		return true;
	}
	
	final void setContext(String contextName){
		if(StringUtils.isBlank(contextName)){
			return;
		}
		this.contextName = contextName;
		this.log = LoggerFactory.getLogger(contextName);
	}
	
}
