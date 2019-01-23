package com.fom.context;

import java.io.File;

import org.apache.log4j.Logger;

import com.fom.context.config.Config;
import com.fom.context.config.ConfigManager;
import com.fom.context.exception.WarnException;
import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public abstract class Context<E extends Config> extends Thread {

	protected final Logger log;

	/**
	 * 模块名称
	 */
	protected final String name;
	
	/**
	 * 目标资源文件的uri
	 */
	protected final String uri;
	
	/**
	 * new File(uri)
	 */
	protected File srcFile;
	
	/**
	 * new File(uri).getName()
	 */
	protected String srcName;

	protected Context(String name, String uri) { 
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
		this.uri = uri;
		this.srcFile = new File(uri);
		this.srcName = srcFile.getName();
	}

	/**
	 * 获取最新的config
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected final E getRuntimeConfig(){
		return (E)ConfigManager.get(name);
	}

	@Override
	public final void run(){
		E config = getRuntimeConfig();
		if(config == null || !config.isRunning()){
			log.info("任务已取消."); 
			return;
		}
		
		this.setName(config.getType() + "[" + srcName + "]");
		long sTime = System.currentTimeMillis();
		try {
			onStart(config);

			exec(config);

			onComplete(config);
			log.info(config.getTypeName() + "任务结束, 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		} catch(WarnException e){
			log.warn(config.getTypeName() + "任务错误结束[" + e.getMessage() + "], 耗时=" + (System.currentTimeMillis() - sTime + "ms"));
		} catch (InterruptedException e) {
			log.warn(config.getTypeName() + "任务中断[" + e.getMessage() + "], 耗时=" + (System.currentTimeMillis() - sTime + "ms"));
		} catch(Throwable e) {
			log.error(config.getTypeName() + "任务异常结束, 耗时=" + (System.currentTimeMillis() - sTime + "ms"), e);
		} finally{
			onFinally();
		}
	}

	/**
	 * 在文件处理前时执行的动作
	 */
	protected void onStart(E config) throws Exception {
		log.info("ready to process the file,if you want to do something before it,"
				+ "you could override the method:[void onStart(E config) throws Exception]");
	}

	protected void exec(E config) throws Exception {
		log.warn("nothing to do,if you want to do something with the file,"
				+ "you should override the method:[void exec(E config) throws Exception]");
	}

	/**
	 * 在文件处理完成时执行的动作
	 */
	protected void onComplete(E config) throws Exception {
		log.info("the file process completed,if you want to do something after it,"
				+ "you could override the method:[void onComplete(E config) throws Exception]");
	}

	void onFinally() {
		
	}

}
