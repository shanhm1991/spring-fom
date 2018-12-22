package com.fom.util.log;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
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
		String root = System.getProperty("webapp.root");
		appender.setFile(root + File.separator + "log" + File.separator + name + ".log");
		appender.setRolling("false"); 
		appender.activateOptions();
		logger.addAppender(appender);  
		return logger;
	}
}
