package com.fom.context;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 */
public abstract class Executor implements Callable<Boolean> {

	protected final Logger log;

	protected final String sourceName;
	
	private final String executorName;
	
	/**
	 * @param name 
	 * Executor根据name找到对应的logger并打印日志信息，如果name为空，则将日志信息打印到根日志中去
	 * @param sourceName 
	 * Executor根据sourceName命名执行线程的名称，Thread.name=new File(sourceName).getName()
	 */
	public Executor(String name, String sourceName) { 
		this.sourceName = sourceName;
		this.executorName = new File(sourceName).getName();
		if(StringUtils.isBlank(name)){
			this.log = Logger.getRootLogger();
		}else{
			this.log = LoggerFactory.getLogger(name);
		}
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
			log.error("任务异常, 耗时=" + (System.currentTimeMillis() - sTime + "ms"), e);
			throw e;
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

	/**
	 * 回调执行
	 * @param result
	 */
	public void callback(boolean result) throws Exception {

	}

}
