package org.springframework.fom.support.service;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.fom.ScheduleInfo;
import org.springframework.fom.support.Response;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public interface FomService extends ScheduleService {

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
	 * @param statDay
	 * @return
	 * @throws ParseException
	 */
	Map<String, Object> getSuccessStat(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName, String statDay) throws ParseException;
	
	/**
	 * 保存统计配置
	 * @param scheduleName
	 * @param statDay
	 * @param statLevel
	 * @param saveDay
	 * @return
	 * @throws ParseException
	 */
	Map<String, Object> saveStatConf(
			@NotBlank(message = "scheduleName cannot be empty.") String scheduleName, 
			@NotBlank(message = "statDay cannot be empty.") String statDay, 
			@NotBlank(message = "statLevel cannot be empty.") String statLevel, 
			@Min(value=1, message="saveDay cannot be less than 1") int saveDay) throws ParseException;
	
	/**
	 * 获取失败的任务统计
	 * @param scheduleName
	 * @return
	 */
	List<Map<String, String>> getFailedStat(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName);
	
	/**
	 * 更新配置 
	 * @param scheduleName
	 * @param map
	 * @throws Exception 
	 */
	void saveConfig(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName, HashMap<String, Object> map) throws Exception;
	
	/**
	 * 导出任务统计
	 * @param scheduleName
	 */
	String buildExport(@NotBlank(message = "scheduleName cannot be empty.") String scheduleName);
}
