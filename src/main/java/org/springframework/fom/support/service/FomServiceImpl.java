package org.springframework.fom.support.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.ScheduleInfo;
import org.springframework.fom.logging.LogLevel;
import org.springframework.fom.logging.LoggerConfiguration;
import org.springframework.fom.logging.LoggingSystem;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Validated
public class FomServiceImpl implements FomService, ApplicationContextAware{

	private ApplicationContext applicationContext;

	private final Map<String, ScheduleContext<?>> scheduleMap = new HashMap<>();

	private static LoggingSystem loggingSystem; 

	static{
		try{
			loggingSystem = LoggingSystem.get(FomServiceImpl.class.getClassLoader());
		}catch(IllegalStateException e){
			System.err.println(e.getMessage()); 
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@PostConstruct 
	private void init(){
		String[] scheduleNames = applicationContext.getBeanNamesForType(ScheduleContext.class);
		for(String scheduleName : scheduleNames){
			scheduleMap.put(scheduleName, (ScheduleContext<?>)applicationContext.getBean(scheduleName));
		}
	}

	@Override
	public List<ScheduleInfo> list() {
		List<ScheduleInfo> list = new ArrayList<>(scheduleMap.size());
		for(ScheduleContext<?> schedule : scheduleMap.values()){
			list.add(schedule.getScheduleInfo());
		}
		return list;
	}

	@Override
	public ScheduleInfo info(String scheduleName) {
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName); 
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule.getScheduleInfo();
	}

	@Override
	public ScheduleInfo info(Class<?> clazz) {
		if(ScheduleContext.class.isAssignableFrom(clazz)){ 
			ScheduleContext<?> scheduleContext = (ScheduleContext<?>)applicationContext.getBean(clazz);
			Assert.notNull(scheduleContext, "schedule of " + clazz + " not exist.");
			return scheduleContext.getScheduleInfo();
		}

		String[] beanNames = applicationContext.getBeanNamesForType(clazz);
		Assert.isTrue(beanNames != null && beanNames.length == 1, "cannot determine schedule by class:" + clazz); 

		String beanName = beanNames[0];
		ScheduleContext<?> scheduleContext = (ScheduleContext<?>)applicationContext.getBean("$" + beanName);
		Assert.notNull(scheduleContext, "schedule of " + clazz + " not exist.");

		return scheduleContext.getScheduleInfo();
	}

	@Override
	public String getLoggerLevel(String scheduleName) {
		if(loggingSystem == null){
			throw new UnsupportedOperationException("No suitable logging system located");
		}

		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		
		String loggerName = schedule.getLogger().getName();
		LoggerConfiguration loggerConfiguration = loggingSystem.getLoggerConfiguration(loggerName);
		if(loggerConfiguration != null){
			LogLevel logLevel = loggerConfiguration.getConfiguredLevel();
			if(logLevel != null){
				return logLevel.name();
			}
			return "INFO";
		}else{
			// 随便找一个配置了的父Logger的级别
			List<LoggerConfiguration> list =loggingSystem.getLoggerConfigurations();
			for(LoggerConfiguration logger : list){
				String name = logger.getName();
				if(name.startsWith(loggerName)){
					LogLevel logLevel = logger.getConfiguredLevel();
					if(logLevel != null){
						return logLevel.name();
					}
				}
			}
			return "INFO";
		}
	}

	@Override
	public void setLoggerLevel(
			@NotBlank(message = "scheduleName cannot be empty.") String scheduleName,
			@NotBlank(message = "levelName cannot be empty.") String levelName) {
		if(loggingSystem == null){
			throw new UnsupportedOperationException("No suitable logging system located");
		}
		
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		
		String loggerName = schedule.getLogger().getName();
		try{
			LogLevel level = LogLevel.valueOf(levelName);
			loggingSystem.setLogLevel(loggerName, level);
		}catch(IllegalArgumentException e){
			throw new UnsupportedOperationException(levelName + " is not a support LogLevel.");
		}
	}
}
