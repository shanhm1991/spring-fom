package org.springframework.fom.boot.test.controller;

import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.ScheduleInfo;
import org.springframework.fom.Task;
import org.springframework.fom.support.Response;
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

	// 注入fom提供的接口服务
	@Autowired
	private FomService fomService;

	// 注入所有fom定时器
	//@Autowired
	//private List<ScheduleContext<?>> schedules;

	// 注入指定的定时器
	// 定时器注册的beanName在目标类的beanName基础上添加了一个$，当然如果目标直接继承ScheduleContext实现，则可以不用
	// testExecutor在下面4.示例中定义
	@Autowired
	private ScheduleContext<Long> testExecutor;

	@RequestMapping("/list")
	@ResponseBody
	public Response<List<ScheduleInfo>> list(){
		return new Response<>(Response.SUCCESS, "", fomService.list());
	}

	@RequestMapping("/task/submit")
	@ResponseBody
	public Response<Void> submit(){
		Task<Long> task = new Task<Long>(){
			@Override
			public Long exec() throws Exception {
				long sleep = RandomUtils.nextLong(3000, 6000);
				Thread.sleep(sleep);
				return sleep;
			}
		};

		testExecutor.submit(task);
		return new Response<>(Response.SUCCESS, "");
	}
}
