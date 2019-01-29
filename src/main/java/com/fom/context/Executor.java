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
public abstract class Executor implements Callable<Boolean> {

	protected volatile Logger log = Logger.getRootLogger();

	protected volatile String name;

	protected final String sourceUri;

	protected ExceptionHandler exceptionHandler;

	protected ResultHandler resultHandler;

	private Throwable e;
	
	private long cost;

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
	public final Boolean call() throws Exception {  
		Thread.currentThread().setName("[" + sourceUri + "]");
		boolean result = true;
		long sTime = System.currentTimeMillis();
		try {
			result = onStart() && exec() && onComplete();
			this.cost = System.currentTimeMillis() - sTime;
			if(result){
				log.info("任务完成, 耗时=" + cost + "ms");
			}else{
				log.warn("任务失败, 耗时=" + cost + "ms");
			}
		} catch(Throwable e) {
			this.e = e;
			this.cost = System.currentTimeMillis() - sTime;
			log.error("任务异常, 耗时=" + cost, e);
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
	 */
	protected boolean onStart() throws Exception {
		log.info("ready to process the file,if you want to do something before it,"
				+ "you could override the method:[boolean onStart(E config) throws Exception]");
		return true;
	}

	/**
	 * 文件处理
	 * @param config
	 * @throws Exception
	 */
	protected abstract boolean exec() throws Exception;

	/**
	 * 在文件处理完成时执行的动作
	 */
	protected boolean onComplete() throws Exception {
		log.info("the file process completed,if you want to do something after it,"
				+ "you could override the method:[boolean onComplete(E config) throws Exception]");
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
	
	final Throwable getThrowable(){
		return e;
	}

	 final long getCost(){
		return cost;
	}
}
