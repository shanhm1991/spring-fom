package com.fom.context;

import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 针对资源uri的执行器
 * 
 * @author shanhm
 *
 */
public abstract class Executor implements Callable<Result> {

	protected volatile Logger log = Logger.getRootLogger();

	protected volatile String name;

	protected final String sourceUri;

	protected ExceptionHandler exceptionHandler;

	protected ResultHandler resultHandler;

	/**
	 * @param sourceUri 资源uri
	 */
	public Executor(String sourceUri) { 
		this.sourceUri = sourceUri;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param exceptionHandler ExceptionHandler
	 */
	public Executor(String sourceUri, ExceptionHandler exceptionHandler) { 
		this(sourceUri);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param resultHandler ResultHandler
	 */
	public Executor(String sourceUri, ResultHandler resultHandler) { 
		this(sourceUri);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public Executor(String sourceUri, ExceptionHandler exceptionHandler, ResultHandler resultHandler) { 
		this(sourceUri);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	public final Result call() throws Exception {  
		Thread.currentThread().setName("[" + sourceUri + "]");
		Result result = new Result(sourceUri); 
		long sTime = System.currentTimeMillis();
		result.startTime = sTime;
		try {
			boolean res = onStart() && exec() && onComplete();
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
	 * 在文件处理前时执行的动作
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected boolean onStart() throws Exception {
		return true;
	}

	/**
	 * 文件处理
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected abstract boolean exec() throws Exception;

	/**
	 * 在文件处理完成时执行的动作
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected boolean onComplete() throws Exception {
		return true;
	}
	
	public final String getName(){
		return name;
	}

	final void setName(String name){
		if(StringUtils.isBlank(name)){
			return;
		}
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
	}
	
}
