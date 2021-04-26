package org.springframework.fom.logging.log4j;

import java.util.Enumeration;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.fom.logging.LogFile;
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
		return null;
	}

	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		
	}

	@SuppressWarnings("unchecked")
	public String getLogLevel(String loggerName) {
		Logger logger = LogManager.getLoggerRepository().exists(loggerName);
		if(logger == null){
			return "NULL";
		}

		Level level = logger.getLevel();
		if(level != null){
			return level.toString();
		}

		Enumeration<Logger> loggerEnumeration = LogManager.getLoggerRepository().getCurrentLoggers();
		while(loggerEnumeration.hasMoreElements()){
			logger = loggerEnumeration.nextElement();
			if((level = logger.getLevel()) != null && logger.getName().startsWith(loggerName)){
				return level.toString();
			}
		}
		
		logger = LogManager.getRootLogger();
		if((level = logger.getLevel()) != null){
			return level.toString();
		}
		return "NULL";
	}

	public void setLogLevel(String loggerName, String levelName) {
		Logger logger = LogManager.getLoggerRepository().exists(loggerName);
		if(logger == null){
			return;
		}
		Level level = Level.toLevel(levelName, Level.INFO);
		logger.setLevel(level);
	}
}
