package org.springframework.fom.support.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.fom.ScheduleInfo;
import org.springframework.fom.support.FomResponse;
import org.springframework.fom.support.FomResponse.Page;
import org.springframework.fom.support.service.FomService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@RequestMapping("/fom")
public class FomController {
	
	@Autowired
	private FomService fomService;

	@RequestMapping("/schedule/list")
	@ResponseBody
	public FomResponse<Page<ScheduleInfo>> list() {
		List<ScheduleInfo> list = fomService.list();
		return new FomResponse<>(FomResponse.SUCCESS, "", new Page<>(list, list.size()));
	}
	
	@RequestMapping("/schedule/info")
	@ResponseBody
	public FomResponse<ScheduleInfo> info(String scheduleName) {
		return new FomResponse<>(FomResponse.SUCCESS, "", fomService.info(scheduleName));
	}
	
	@RequestMapping("/schedule/logger/level")
	@ResponseBody
	public FomResponse<String> loggerLevel(String scheduleName) {
		return new FomResponse<>(FomResponse.SUCCESS, "", fomService.getLoggerLevel(scheduleName));
	}
	
	@RequestMapping("/schedule/logger/level/update")
	@ResponseBody
	public FomResponse<Void> updateloggerLevel(String scheduleName, String levelName) {
		fomService.updateloggerLevel(scheduleName, levelName);
		return new FomResponse<>(FomResponse.SUCCESS, "");
	}
	
	@RequestMapping("/schedule/start")
	@ResponseBody
	public FomResponse<Void> start(String scheduleName) {
		return fomService.start(scheduleName);
	}
	
	@RequestMapping("/schedule/shutdown")
	@ResponseBody
	public FomResponse<Void> shutdown(String scheduleName) {
		return fomService.shutdown(scheduleName);
	}
	
	@RequestMapping("/schedule/exec")
	@ResponseBody
	public FomResponse<Void> exec(String scheduleName) {
		return fomService.exec(scheduleName);
	}
	
	@RequestMapping("/schedule/waitings")
	@ResponseBody
	public FomResponse<Map<String, String>> waitingTasks(String scheduleName) {
		return new FomResponse<>(FomResponse.SUCCESS, "", fomService.getWaitingTasks(scheduleName));
	}
	
	@RequestMapping("/schedule/actives")
	@ResponseBody
	public FomResponse<List<Map<String, String>>> activeTasks(String scheduleName) {
		return new FomResponse<>(FomResponse.SUCCESS, "", fomService.getActiveTasks(scheduleName));
	}
	
	@RequestMapping("/schedule/export")
	public void statExport(String scheduleName, HttpServletResponse resp) throws IOException { 
		String stat = fomService.buildExport(scheduleName);
		resp.reset();
		resp.setContentType("application/octet-stream;charset=UTF-8");
		resp.addHeader("Content-Disposition", "attachment;filename=\"" + scheduleName + "." + System.currentTimeMillis() +".txt\"");
		try(PrintWriter write = resp.getWriter()){
			write.write(stat);
			write.flush();
		}
	}
	
	@RequestMapping("/schedule/faileds")
	@ResponseBody
	public FomResponse<List<Map<String, String>>> failedStat(String scheduleName) {
		return new FomResponse<>(FomResponse.SUCCESS, "", fomService.getFailedStat(scheduleName));
	}
	
	@RequestMapping("/schedule/success")
	@ResponseBody
	public FomResponse<Map<String, Object>> successStat(String scheduleName, String statDay) throws ParseException {
		return new FomResponse<>(FomResponse.SUCCESS, "", fomService.getSuccessStat(scheduleName, statDay));
	}
	
	@RequestMapping("/schedule/saveStatConf")
	@ResponseBody
	public FomResponse<Map<String, Object>> saveStatConf(String scheduleName,
														 String statDay, String statLevel, int saveDay) throws ParseException {
		return new FomResponse<>(FomResponse.SUCCESS, "", fomService.saveStatConf(scheduleName, statDay, statLevel, saveDay));
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping("/schedule/config/save")
	@ResponseBody
	public FomResponse<Void> saveConfig(String scheduleName, String data) throws Exception {
		HashMap<String, Object> configMap = (HashMap<String, Object>) new ObjectMapper().readValue(data, HashMap.class);
		fomService.saveConfig(scheduleName, configMap);
		return new FomResponse<>(FomResponse.SUCCESS, "");
	}
}
