package com.demo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fom.context.FomService;
import com.fom.context.Task;

@RestController
public class DemoController {
	
	@Autowired
	@Qualifier("fomService")
	private FomService service;

	public void setService(FomService service) {
		this.service = service;
	}
	
	@RequestMapping("/demo")
	@ResponseBody
	public Map<String,Object> demo() throws Exception{
		Task task = new Task("submitTask"){
			@Override
			protected boolean exec() throws Exception {
				Thread.sleep(4000); 
				return true;
			}
			
		};
		service.submitTask("DemoStateTest1", task);
		
		Map<String,Object> map = new HashMap<>();
		map.put("result", true);
		return map;
	}

}
