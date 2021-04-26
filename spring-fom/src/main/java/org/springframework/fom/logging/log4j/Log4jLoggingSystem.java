package org.springframework.fom.logging.log4j;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.fom.logging.LogFile;
import org.springframework.fom.logging.LogLevel;
import org.springframework.fom.logging.LoggerConfiguration;
import org.springframework.fom.logging.LoggingInitializationContext;
import org.springframework.fom.logging.Slf4JLoggingSystem;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class Log4jLoggingSystem extends Slf4JLoggingSystem {
	

	public Log4jLoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	protected String[] getStandardConfigLocations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		// TODO Auto-generated method stub
	}
	
	public void hasLog(String name){
		Enumeration loggerEnumeration = LogManager.getLoggerRepository().getCurrentLoggers();
		while(loggerEnumeration.hasMoreElements()){
			Logger logger = (Logger)loggerEnumeration.nextElement();
			System.out.println(logger.getName()); 
		}
	}

	@Override
	public List<LoggerConfiguration> getLoggerConfigurations() {
		return new ArrayList<>();
	}
	
	@Override
	public LoggerConfiguration getLoggerConfiguration(String loggerName) {
		return null;
	}
	
	@Override
	public void setLogLevel(String loggerName, LogLevel logLevel) {
		
	}
}
