package org.springframework.fom.boot.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.ScheduleInfo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Controller
@RequestMapping("/fom")
public class TestController {
	
	@Autowired
	private List<ScheduleContext<?>> schedules;

	@RequestMapping("/list")
	@ResponseBody
	public List<ScheduleInfo> test() throws Exception{
		List<ScheduleInfo> infos = new ArrayList<>(schedules.size());
		for(ScheduleContext<?> schedule : schedules){
			infos.add(schedule.getScheduleInfo());
		}
		return infos;
	}
}
