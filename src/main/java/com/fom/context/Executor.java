package com.fom.context;

import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 针对source的任务执行者
 * 
 * @author shanhm
 *
 */
public abstract class Executor implements Callable<Result> {

	protected volatile Logger log = Logger.getRootLogger();

	protected volatile String contextName;

	protected final String source;

	protected ExceptionHandler exceptionHandler;

	protected ResultHandler resultHandler;

	/**
	 * @param source 创建Executor的资源
	 */
	public Executor(String source) { 
		this.source = source;
	}

	/**
	 * @param source 创建Executor的资源
	 * @param exceptionHandler ExceptionHandler
	 */
	public Executor(String source, ExceptionHandler exceptionHandler) { 
		this(source);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param source 创建Executor的资源
	 * @param resultHandler ResultHandler
	 */
	public Executor(String source, ResultHandler resultHandler) { 
		this(source);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param source 创建Executor的资源
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public Executor(String source, ExceptionHandler exceptionHandler, ResultHandler resultHandler) { 
		this(source);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	public final Result call() throws Exception {  
		Thread.currentThread().setName(source);
		Result result = new Result(source); 
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
	
	/**
	 * 如果单独使用Executor，将返回null，只有在context中使用时才有值返回
	 * @return context name
	 */
	protected final String getContextName(){
		return contextName;
	}

	final void setContext(String contextName){
		if(StringUtils.isBlank(contextName)){
			return;
		}
		this.contextName = contextName;
		this.log = LoggerFactory.getLogger(contextName);
	}
	
}
