package com.fom.context;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 针对sourceUri的执行器
 * 
 * @author shanhm
 *
 */
public abstract class Executor implements Callable<Boolean> {

	protected final Logger log;

	protected final String sourceName;

	private final String executorName;

	private final String name;
	
	protected ExceptionHandler exceptionHandler;

	protected ResultHandler resultHandler;

	private Throwable e;

	/**
	 * @param name 模块名称
	 * @param sourceName 资源名称
	 */
	public Executor(String name, String sourceName) { 
		this.sourceName = sourceName;
		this.executorName = new File(sourceName).getName();
		if(StringUtils.isBlank(name)){
			this.log = Logger.getRootLogger();
			this.name = this.getClass().getSimpleName();
		}else{
			this.name = name;
			this.log = LoggerFactory.getLogger(name);
		}
	}

	/**
	 * @param name 模块名称
	 * @param sourceName 资源名称
	 * @param exceptionHandler ExceptionHandler
	 */
	public Executor(String name, String sourceName, ExceptionHandler exceptionHandler) { 
		this(name, sourceName);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param name
	 * @param sourceName
	 * @param resultHandler ResultHandler
	 */
	public Executor(String name, String sourceName, ResultHandler resultHandler) { 
		this(name, sourceName);
		this.resultHandler = resultHandler;
	}
	
	/**
	 * @param name
	 * @param sourceName
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public Executor(String name, String sourceName, ExceptionHandler exceptionHandler, ResultHandler resultHandler) { 
		this(name, sourceName);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	public final Boolean call() throws Exception {  
		Thread.currentThread().setName("[" + executorName + "]");
		boolean result = true;
		long sTime = System.currentTimeMillis();
		try {
			result = onStart() && exec() && onComplete();
			if(result){
				log.info("任务完成, 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
			}else{
				log.warn("任务失败, 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
			}
		} catch(Throwable e) {
			this.e = e;
			log.error("任务异常, 耗时=" + (System.currentTimeMillis() - sTime + "ms"), e);
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

	public final long getCost(){
		return 0;
	}

	final String getMsg(){
		if(e != null){
			return e.getMessage();
		}
		return "";
	}
}
