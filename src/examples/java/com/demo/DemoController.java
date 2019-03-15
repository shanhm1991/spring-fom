package com.demo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fom.context.Context;
import com.fom.context.Task;

@RestController
public class DemoController {
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping("/demo")
	@ResponseBody
	public Map<String,Object> demo() throws Exception{
		Task task = new Task<Boolean>("submitTask"){
			@Override
			protected Boolean exec() throws Exception {
				Thread.sleep(4000); 
				return true;
			}
			
		};
		Context.submitTask("DemoStateTest1", task);
		
		Map<String,Object> map = new HashMap<>();
		map.put("result", true);
		return map;
	}

}
