package org.springframework.fom.support.service;

import java.text.ParseException;
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
import org.springframework.fom.ScheduleStatistics;
import org.springframework.fom.logging.LogLevel;
import org.springframework.fom.logging.LoggerConfiguration;
import org.springframework.fom.logging.LoggingSystem;
import org.springframework.fom.logging.log4j.Log4jLoggingSystem;
import org.springframework.fom.support.Response;
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
			ScheduleInfo scheduleInfo = schedule.getScheduleInfo();
			scheduleInfo.setLoggerLevel(getLoggerLevel(schedule.getScheduleName())); 
			list.add(scheduleInfo);
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
		Assert.notNull(loggingSystem, "No suitable logging system located");

		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");

		String loggerName = schedule.getLogger().getName();
		if(loggingSystem instanceof Log4jLoggingSystem){
			Log4jLoggingSystem log4jLoggingSystem = (Log4jLoggingSystem)loggingSystem;
			return log4jLoggingSystem.getLogLevel(loggerName);
		}

		LoggerConfiguration loggerConfiguration = loggingSystem.getLoggerConfiguration(loggerName);
		if(loggerConfiguration != null){
			LogLevel logLevel = loggerConfiguration.getConfiguredLevel();
			if(logLevel != null){
				return logLevel.name();
			}
			return "NULL";
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
			return "NULL";
		}
	}

	@Override
	public void updateloggerLevel(
			@NotBlank(message = "scheduleName cannot be empty.") String scheduleName,
			@NotBlank(message = "levelName cannot be empty.") String levelName) {
		Assert.notNull(loggingSystem, "No suitable logging system located");

		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");

		String loggerName = schedule.getLogger().getName();
		if(loggingSystem instanceof Log4jLoggingSystem){
			Log4jLoggingSystem log4jLoggingSystem = (Log4jLoggingSystem)loggingSystem;
			log4jLoggingSystem.setLogLevel(loggerName, levelName);
			return;
		}

		try{
			LogLevel level = LogLevel.valueOf(levelName);
			loggingSystem.setLogLevel(loggerName, level);
		}catch(IllegalArgumentException e){
			throw new UnsupportedOperationException(levelName + " is not a support LogLevel.");
		}
	}

	@Override
	public Response<Void> start(String scheduleName) {
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule.scheduleStart();
	}

	@Override
	public Response<Void> shutdown(String scheduleName){
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule.scheduleShutdown();
	}

	@Override
	public Response<Void> exec(String scheduleName) {
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule.scheduleExecNow();
	}
	
	@Override
	public Map<String, String> getWaitingTasks(String scheduleName) { 
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule.getWaitingTasks();
	}

	@Override
	public List<Map<String, String>> getActiveTasks(String scheduleName) {
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule.getActiveTasks();
	}

	@Override
	public Map<String, Object> getSuccessStat(String scheduleName, String statDay) throws ParseException { 
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule.getSuccessStat(statDay);
	}

	@Override
	public Map<String, Object> saveStatConf(String scheduleName, String statDay, String statLevel, int saveDay) throws ParseException {
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		
		Map<String, Object> successStat = null;
		ScheduleStatistics scheduleStatistics = schedule.getScheduleStatistics();
		if(scheduleStatistics.setLevel(statLevel.split(","))){
			successStat = schedule.getSuccessStat(statDay);
		}
		
		scheduleStatistics.setSaveDay(saveDay); 
		return successStat;
	}
	
	@Override
	public List<Map<String, String>> getFailedStat(String scheduleName){
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule.getFailedStat();
	}
}
