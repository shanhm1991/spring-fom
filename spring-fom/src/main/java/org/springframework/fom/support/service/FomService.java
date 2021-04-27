package org.springframework.fom.support.service;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.fom.ScheduleInfo;
import org.springframework.fom.support.Response;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public interface FomService {

	/**
	 * 获取所有schedule信息
	 * @return
	 */
	List<ScheduleInfo> list();
	
	/**
	 * 获取schedule信息
	 * @param scheduleName
	 * @return
	 */
	ScheduleInfo info(@NotBlank(message="scheduleName cannot be empty.") String scheduleName);
	
	/**
	 * 获取schedule信息
	 * @param clazz
	 * @return
	 */
	ScheduleInfo info(@NotNull(message="scheduleClass cannot be null.") Class<?> clazz);
	
	/**
	 * 获取schedule的日志级别
	 * @param scheduleName
	 * @return
	 */
	String getLoggerLevel(@NotBlank(message="scheduleName cannot be empty.") String scheduleName);
	
	/**
	 * 设置schedule的日志级别
	 * @param scheduleName
	 * @return
	 */
	void updateloggerLevel(
			@NotBlank(message="scheduleName cannot be empty.") String scheduleName,
			@NotBlank(message="levelName cannot be empty.") String levelName);
	
	/**
	 * schedule启动
	 * @param scheduleName
	 * @return
	 */
	Response<Void> start(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName);
	
	/**
	 * schedule停止
	 * @param scheduleName
	 * @return
	 */
	Response<Void> shutdown(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName);
	
	/**
	 * schedule立即执行
	 * @param scheduleName
	 * @return
	 */
	Response<Void> exec(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName);
	
	/**
	 * 获取正在等待的任务
	 * @param scheduleName 
	 * @return 
	 */
	Map<String, String> getWaitingTasks(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName);
	
	/**
	 * 获取正在执行的任务
	 * @param scheduleName 
	 * @return 
	 */
	List<Map<String, String>> getActiveTasks(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName);
	
	/**
	 *  获取成功的任务统计
	 * @param scheduleName
	 * @param endDay
	 * @return
	 * @throws ParseException
	 */
	Map<String, Object> getSuccessStat(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName, String endDay) throws ParseException;
}
