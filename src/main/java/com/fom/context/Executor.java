package com.fom.context;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import com.fom.util.exception.WarnException;
import com.fom.util.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public abstract class Executor<E extends Config> extends Thread {

	E config;
	
	DecimalFormat numFormat  = new DecimalFormat("0.0");

	protected final Logger log;

	protected final String name;

	protected final String srcPath;

	protected final File srcFile;

	protected final double srcSize;

	protected final String srcName;

	protected Executor(String name, String path) { 
		this.name = name;
		this.srcPath = path;
		this.srcFile = new File(path);
		this.srcName = srcFile.getName();
		this.srcSize = srcFile.length() / 1024.0;
		Config config = getRuntimeConfig();
		if(config == null){
			throw new RuntimeException("任务取消.");
		}
		this.setName(config.getType() + "[" + srcName + "]");
		this.log = LoggerFactory.getLogger(config.getType() + "." + name);
	}

	/**
	 * 获取最新的config
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected final E getRuntimeConfig(){
		return (E)ConfigManager.getConfig(name);
	}

	@Override
	public final void run(){
		config = getRuntimeConfig();
		if(config == null || !config.isRunning){
			log.info("任务取消."); 
			return;
		}
		Thread.currentThread().setName(config.getType() + "[" + srcName + "]");
		long sTime = System.currentTimeMillis();
		try {
			onStart(config);
			
			execute();
			
			onComplete(config);
			log.info(config.getTypeName() + "任务结束, 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		} catch(WarnException e){
			log.warn(config.getTypeName() + "任务错误结束[" + e.getMessage() + "], 耗时=" + (System.currentTimeMillis() - sTime + "ms"));
		} catch (InterruptedException e) {
			//检测点:impoter每次batchProcessLineData之前
			log.warn(config.getTypeName() + "任务中断[" + e.getMessage() + "], 耗时=" + (System.currentTimeMillis() - sTime + "ms"));
		} catch(Throwable e) {
			log.error(config.getTypeName() + "任务异常结束, 耗时=" + (System.currentTimeMillis() - sTime + "ms"), e);
		} finally{
			onFinally();
		}
	}

	/**
	 * 继承自Executor，在任务线程启动时执行的第一个动作，可以完成一些准备操作
	 */
	protected void onStart(E config) throws Exception {

	}

	abstract void execute() throws Exception;
	
	/**
	 * 继承自Executor，在任务线程完成时执行的动作
	 */
	protected void onComplete(E config) throws Exception {

	}

	void onFinally() {

	}

}
