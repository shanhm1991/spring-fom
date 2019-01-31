package com.fom.context;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.fom.log.LoggerAppender;
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
				log.info("任务完成, 耗时=" + cost + "ms");
			}else{
				log.warn("任务失败, 耗时=" + cost + "ms");
			}
			result.result = res;
			result.costTime = cost;
		} catch(Throwable e) {
			long cost = System.currentTimeMillis() - sTime;
			log.error("任务异常, 耗时=" + cost, e);
			result.result = false;
			result.throwable = e;
			if(exceptionHandler != null){
				exceptionHandler.handle(e); 
			}
		}
		
		record(name, result);
		if(resultHandler != null){
			resultHandler.handle(result);
		}
		return result;
	}

	/**
	 * 在文件处理前时执行的动作
	 */
	protected boolean onStart() throws Exception {
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
		return true;
	}
	
	private static void record(String name, Result result){
		String logName = name + ".record";
		Logger logger = LogManager.exists(logName);
		if(logger == null){
			logger = Logger.getLogger(logName); 
			logger.setLevel(Level.INFO);  
			logger.setAdditivity(false); 
			logger.removeAllAppenders();
			LoggerAppender appender = new LoggerAppender();
			PatternLayout layout = new PatternLayout();  
			layout.setConversionPattern("%m%n");  
			appender.setLayout(layout); 
			appender.setEncoding("UTF-8");
			appender.setAppend(true);
			if(StringUtils.isBlank(System.getProperty("log.root"))){
				appender.setFile("log" + File.separator + logName);
			}else{
				appender.setFile(System.getProperty("log.root") + File.separator + logName);
			}
			appender.setRolling("false"); 
			appender.activateOptions();
			logger.addAppender(appender); 
		}
		StringBuilder builder = new StringBuilder("sourceUri=" + result.sourceUri
				+ ", result=" + result.result
				+ ", startTime=" + result.startTime
				+ ", costTime=" + result.costTime);
		if(result.throwable == null){
			builder.append(", Throwable=null");
		}else{
			builder.append(", Throwable=" + result.throwable.getMessage());
		}
		logger.error(builder.toString()); 
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
