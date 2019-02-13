package com.fom.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fom.context.FomService;

/**
 * 
 * @author shanhm
 *
 */
@Controller
public class FomController {

	@Autowired
	@Qualifier("fomService")
	private FomService service;

	public void setService(FomService service) {
		this.service = service;
	}
	 
	@RequestMapping("/list")
	@ResponseBody
	public Map<String, Object> list() throws Exception{
		Map<String, Object> a = service.list();
		return a;
	}
	
	@RequestMapping("/save")
	@ResponseBody
	public Map<String,Object> save(String name, String data) throws Exception{ 
		return service.save(name, data);
	}
	
	@RequestMapping("/operation")
	@ResponseBody
	public Map<String,Object> operation(String name, int opid) throws Exception{ 
		switch(opid){
		case 1: return service.start(name);
		case 2: return service.stop(name);
		case 3: return service.interrupt(name);
		default : 
			Map<String,Object> map = new HashMap<>();
			map.put("result", false);
			map.put("msg", "unsupported operation.");
			return map;
		}
	}
	
	@RequestMapping("/state")
	@ResponseBody
	public Map<String,Object> state(String name) throws Exception{ 
		return service.state(name);
	}
	
	@RequestMapping("/log")
	public String log(String name, String level) throws Exception{ 
		service.changeLogLevel(name, level); 
		return "success";
	}
	
	@RequestMapping("/create")
	@ResponseBody
	public Map<String,Object> create(String json) throws Exception{ 
		return service.create(json);
	}
	
	@RequestMapping("/taskdetail")
	@ResponseBody
	public Map<String,Object> taskdetail(String name) throws Exception{ 
		return service.taskdetail(name);
	}
}
