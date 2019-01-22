package com.fom.log;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LoggerFactory {

	public static Logger getLogger(String name){
		Logger logger = LogManager.exists(name);
		if(logger != null){
			return logger;
		}
		logger = Logger.getLogger(name);
		logger.setLevel(Level.INFO);  
		logger.setAdditivity(false); 
		logger.removeAllAppenders();
		LoggerAppender appender = new LoggerAppender();
		PatternLayout layout = new PatternLayout();  
		layout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss SSS} [%p] %t [%F:%L] %m%n");  
		appender.setLayout(layout); 
		appender.setEncoding("UTF-8");
		appender.setAppend(true);
		appender.setFile(System.getProperty("log.root") + File.separator + name + ".log");
		appender.setRolling("false"); 
		appender.activateOptions();
		logger.addAppender(appender);  
		return logger;
	}
}
