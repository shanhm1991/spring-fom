package org.springframework.fom.boot.test.controller;

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
 * 可以在controller中注入FomService以便调用一些接口，或者直接注入ScheduleContext
 * 
 * 也可以通过 @EnableFom(enableFomView = true)，直接访问内置的FomController
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
