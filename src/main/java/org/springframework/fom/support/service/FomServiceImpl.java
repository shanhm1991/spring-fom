package org.springframework.fom.support.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.fom.Result;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.ScheduleInfo;
import org.springframework.fom.ScheduleStatistics;
import org.springframework.fom.annotation.Fom;
import org.springframework.fom.logging.LogLevel;
import org.springframework.fom.logging.LoggerConfiguration;
import org.springframework.fom.logging.LoggingSystem;
import org.springframework.fom.logging.log4j.Log4jLoggingSystem;
import org.springframework.fom.support.FomResponse;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Validated
public class FomServiceImpl implements FomService {

	// file redis
	@Value("${spring.fom.cache.type:file}")
	private String cacheType;
	
	@Value("${spring.fom.cache.history:false}")
	private boolean cacheHistory;

	@Value("${spring.fom.cache.file.path:cache/schedule}")
	private String cacheFilePath;

	@Autowired
	private ApplicationContext applicationContext;

	private static LoggingSystem loggingSystem; 

	static{
		try{
			loggingSystem = LoggingSystem.get(FomServiceImpl.class.getClassLoader());
		}catch(IllegalStateException e){
			System.err.println(e.getMessage()); 
		}
	}

	@Override
	public List<ScheduleInfo> list() {
		List<ScheduleInfo> list = new ArrayList<>();
		String[] names = applicationContext.getBeanNamesForType(ScheduleContext.class);
		if(names.length == 0){
			return list;
		}

		for(String name : names){
			ScheduleContext<?> schedule = applicationContext.getBean(name, ScheduleContext.class);
			ScheduleInfo scheduleInfo = schedule.getScheduleInfo();
			scheduleInfo.setLoggerLevel(getLoggerLevel(schedule.getScheduleName())); 
			list.add(scheduleInfo);
		}
		return list;
	}

	private ScheduleContext<?> getScheduleByValidName(String scheduleName){
		ScheduleContext<?> schedule = applicationContext.getBean(scheduleName, ScheduleContext.class);
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule;
	}

	@Override
	public ScheduleInfo info(String scheduleName) {
		ScheduleContext<?> schedule = getScheduleByValidName(scheduleName);
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
		Assert.isTrue(beanNames.length == 1, "cannot determine schedule by class:" + clazz);

		String beanName = beanNames[0];
		ScheduleContext<?> scheduleContext = (ScheduleContext<?>)applicationContext.getBean("$" + beanName);
		Assert.notNull(scheduleContext, "schedule of " + clazz + " not exist.");

		return scheduleContext.getScheduleInfo();
	}

	@Override
	public String getLoggerLevel(String scheduleName) {
		Assert.notNull(loggingSystem, "No suitable logging system located");
		ScheduleContext<?> schedule = getScheduleByValidName(scheduleName);

		String loggerName = schedule.getLogger().getName();
		if(loggingSystem instanceof Log4jLoggingSystem){
			Log4jLoggingSystem log4jLoggingSystem = (Log4jLoggingSystem)loggingSystem;
			return log4jLoggingSystem.getLogLevel(loggerName);
		}

		LoggerConfiguration loggerConfiguration = loggingSystem.getLoggerConfiguration(loggerName);
		if(loggerConfiguration != null){
			LogLevel logLevel = loggerConfiguration.getConfiguredLevel();
			if(logLevel == null){
				logLevel = loggerConfiguration.getEffectiveLevel();
			}
			if(logLevel != null){
				return logLevel.name();
			}
			return "NULL";
		}else{
			// 向上找一个最近的父Logger
			List<LoggerConfiguration> list =loggingSystem.getLoggerConfigurations();
			for(LoggerConfiguration logger : list){
				String name = logger.getName();
				if(name.startsWith(loggerName)){
					LogLevel logLevel = logger.getConfiguredLevel();
					if(logLevel == null){
						logLevel = logger.getEffectiveLevel();
					}
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
		ScheduleContext<?> schedule = getScheduleByValidName(scheduleName);

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
	public FomResponse<Void> start(String scheduleName) {
		return getScheduleByValidName(scheduleName).scheduleStart();
	}

	@Override
	public FomResponse<Void> shutdown(String scheduleName){
		return getScheduleByValidName(scheduleName).scheduleShutdown();
	}

	@Override
	public FomResponse<Void> exec(String scheduleName) {
		return getScheduleByValidName(scheduleName).scheduleExecNow();
	}

	@Override
	public Map<String, String> getWaitingTasks(String scheduleName) { 
		return getScheduleByValidName(scheduleName).getWaitingTasks();
	}

	@Override
	public List<Map<String, String>> getActiveTasks(String scheduleName) {
		return getScheduleByValidName(scheduleName).getActiveTasks();
	}

	@Override
	public Map<String, Object> getSuccessStat(String scheduleName, String statDay) throws ParseException { 
		return getScheduleByValidName(scheduleName).getSuccessStat(statDay);
	}

	@Override
	public Map<String, Object> saveStatConf(String scheduleName, String statDay, String statLevel, int saveDay) throws ParseException {
		ScheduleContext<?> schedule = getScheduleByValidName(scheduleName);

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
		return getScheduleByValidName(scheduleName).getFailedStat();
	}

	@Override
	public void saveConfig(String scheduleName, HashMap<String, Object> map) throws Exception {
		ScheduleContext<?> schedule = getScheduleByValidName(scheduleName);
		schedule.saveConfig(map, true);
		serializeConfig(schedule);
	}

	private void serializeConfig(ScheduleContext<?> schedule){  
		if("file".equalsIgnoreCase(cacheType)){
			serializeFile(schedule);
		}else if("redis".equalsIgnoreCase(cacheType)){
			// TODO
		}
	}
	
	private void serializeFile(ScheduleContext<?> schedule){
		File dir = new File(cacheFilePath);
		if(!dir.exists() && !dir.mkdirs()){
			throw new IllegalArgumentException("cann't touch cache dir " + cacheFilePath); 
		}
		
		File cacheFile = new File(dir.getPath() + File.separator + schedule.getScheduleName() + ".cache"); 
		if(cacheFile.exists()){
			if(cacheHistory){
				cacheFile.renameTo(new File(dir.getPath() + File.separator + schedule.getScheduleName() + ".cache." + System.currentTimeMillis()));
			}else{
				cacheFile.delete();
			}
		}
		
		Map<String, Object> configMap = schedule.getScheduleConfig().getConfMap();
		
		try{
			String str = new ObjectMapper().writeValueAsString(configMap);
			System.out.println(str);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		try(FileOutputStream out = new FileOutputStream(cacheFile);
				ObjectOutputStream oos = new ObjectOutputStream(out)){
			oos.writeObject(configMap);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public String buildExport(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName) {
		ScheduleContext<?> schedule = getScheduleByValidName(scheduleName);

		ScheduleStatistics scheduleStatistics = schedule.getScheduleStatistics();
		Map<String, List<Result<?>>> success = scheduleStatistics.copySuccessMap();
		Map<String, List<Result<?>>> faield = scheduleStatistics.copyFaieldMap();

		TreeSet<String> daySet = new TreeSet<>();
		daySet.addAll(success.keySet());
		daySet.addAll(faield.keySet());

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss SSS");
		StringBuilder builder = new StringBuilder();
		for(String day : daySet){
			List<Result<?>> slist = success.get(day);
			if(slist != null){
				builder.append(day).append(" success:").append("\n");
				for(Result<?> result : slist){
					builder.append(result.getTaskId()).append(", ");
					builder.append("submitTime=").append(dateFormat.format(result.getSubmitTime())).append(", ");
					builder.append("startTime=").append(dateFormat.format(result.getStartTime())).append(", ");
					builder.append("cost=").append(result.getCostTime()).append("ms, ");
					builder.append("result=").append(result.getContent()).append("\n");
				}
				builder.append("\n");
			}

			List<Result<?>> flist = faield.get(day);
			if(flist != null){
				builder.append(day).append(" failed:").append("\n");
				for(Result<?> result : flist){
					builder.append(result.getTaskId()).append(", ");
					builder.append("submitTime=").append(dateFormat.format(result.getSubmitTime())).append(", ");
					builder.append("startTime=").append(dateFormat.format(result.getStartTime())).append(", ");
					builder.append("cost=").append(result.getCostTime()).append("ms, ");
					Throwable throwable = result.getThrowable();
					if(throwable == null){
						builder.append("cause=null").append("\n");
					}else{
						Throwable cause = throwable;
						while((cause = throwable.getCause()) != null){
							throwable = cause;
						} 

						builder.append("cause=").append(throwable.toString()).append("\n");
						for(StackTraceElement stack : throwable.getStackTrace()){ 
							builder.append(stack).append("\n");
						}
					}
				}
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	private ScheduleContext<?> getCurrentSchedule(){
		StackTraceElement[] stacks = new Exception().getStackTrace();
		try{
			for(StackTraceElement stack : stacks){
				String clazzName = stack.getClassName();
				Class<?> clazz = Class.forName(clazzName); 
				Fom fom = clazz.getAnnotation(Fom.class);
				if(clazz.getAnnotation(Fom.class) != null){
					String beanName =  fom.value();
					if(beanName == null || "".equals(beanName.trim())){ 
						beanName = clazz.getSimpleName();
						beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1, beanName.length());
					}

					if(ScheduleContext.class.isAssignableFrom(clazz)){
						return applicationContext.getBean(beanName, ScheduleContext.class);
					}else{
						return applicationContext.getBean("$" + beanName, ScheduleContext.class);
					}
				}
			}
		}catch(Exception e){
			throw new IllegalStateException("get current schedule failed", e);
		}
		throw new IllegalStateException("cannot get current schedule in the current callstack.");
	}

	@Override
	public void serializeCurrent() {
		serializeConfig(getCurrentSchedule());
	}

	@Override
	public void serialize(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName) {
		serializeConfig(getScheduleByValidName(scheduleName));
	}

	@Override
	public void putCurrentConfig(String key, Object value) {
		getCurrentSchedule().getScheduleConfig().set(key, value);
	}

	@Override
	public void putConfig(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName, String key, Object value) {
		getScheduleByValidName(scheduleName).getScheduleConfig().set(key, value);
	}

	@Override
	public <V> V getCurrentConfig(String key) {
		return getCurrentSchedule().getScheduleConfig().get(key);
	}

	@Override
	public <V> V getConfig(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName, String key) {
		return getScheduleByValidName(scheduleName).getScheduleConfig().get(key);
	}
}
