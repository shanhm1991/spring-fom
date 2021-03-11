package org.eto.fom.util.log;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
//import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 * <br>slf4j将apache的log4j适配成功能更强大的接口，但是没有暴露Logger的一些操作方法
 * <br>这里相当于做了两步操作，先通过apache的log4j创建自定义属性的Logger对象，再将其适配成slf4j的Logger接口
 */
public class SlfLoggerFactory {

	public static Logger getLogger(String name){
		org.apache.log4j.Logger logger = LogManager.exists(name);
		if(logger != null){
			return LoggerFactory.getLogger(name);
		}

		logger = org.apache.log4j.Logger.getLogger(name); 
		logger.setLevel(Level.INFO);  
		logger.setAdditivity(true); 
		//logger.removeAllAppenders();

		final PatternLayout layout = new PatternLayout();  
		layout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss SSS} %17t [%5p] %m%n");  

		final LoggerAppender appender = new LoggerAppender(); 
		appender.setName(name);
		appender.setLayout(layout); 
		appender.setEncoding("UTF-8");
		appender.setAppend(true);
		appender.setFile("log" + File.separator + name + ".log");
		appender.setRolling("false"); 
		appender.activateOptions();
		logger.addAppender(appender);  
		return LoggerFactory.getLogger(name);
	}
}
