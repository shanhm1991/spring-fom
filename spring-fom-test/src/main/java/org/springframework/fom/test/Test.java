package org.springframework.fom.test;

import org.slf4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.fom.logging.LoggingSystem;
import org.springframework.fom.logging.log4j.Log4jLoggingSystem;

/**
 * 
 * @author shanhm1991@163.com
 *
 */ 
public class Test {
	
	private static Logger log = org.slf4j.LoggerFactory.getLogger(Test.class);

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		new AnnotationConfigApplicationContext("org.springframework.fom.test");
		
		Log4jLoggingSystem lg = (Log4jLoggingSystem)LoggingSystem.get(Test.class.getClassLoader());
		lg.hasLog(null);
	}
}
