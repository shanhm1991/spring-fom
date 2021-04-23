package org.springframework.fom.boot.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.ScheduleInfo;
import org.springframework.fom.support.service.FomService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 
 * 可以直接使用内置的FomController，如果不想用可以通过 @EnableFom(enableFomView = false) 屏蔽掉
 * 
 * 如果自定义的Controller想获取ScheduleContext，提供了FomService接口，或者可以直接自己注入ScheduleContext
 * 
 * @author shanhm1991@163.com
 *
 */
@Controller
@RequestMapping("/test")
public class TestController {
	
	@Autowired
	private FomService fomService;
	
	@Autowired
	private List<ScheduleContext<?>> schedules;
	
	@RequestMapping("/list")
	@ResponseBody
	public List<ScheduleInfo> list(){
		return fomService.list();
	}
	
	@RequestMapping("/count")
	@ResponseBody
	public int count(){
		return schedules.size();
	}
}
