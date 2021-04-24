package org.springframework.fom.support.service;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.fom.ScheduleInfo;

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
	void setLoggerLevel(
			@NotBlank(message="scheduleName cannot be empty.") String scheduleName,
			@NotBlank(message="levelName cannot be empty.") String levelName);
}
