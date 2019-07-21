package com.examples.controller;

import java.util.HashMap;
import java.util.Map;

import org.eto.fom.context.ContextUtil;
import org.eto.fom.context.Task;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController {
	
	private static final long DELAY = 4000;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping("/demo")
	@ResponseBody
	public Map<String,Object> demo() throws Exception{
		Task task = new Task<Boolean>("submitTask"){
			@Override
			protected Boolean exec() throws Exception {
				Thread.sleep(DELAY); 
				return true;
			}
			
		};
		ContextUtil.submitTask("DemoStateTest1", task);
		
		Map<String,Object> map = new HashMap<>();
		map.put("result", true);
		return map;
	}

}
