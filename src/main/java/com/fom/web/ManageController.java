package com.fom.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fom.context.ManagerService;

@Controller
public class ManageController {

	@Autowired
	@Qualifier("managerService")
	private ManagerService service;

	public void setService(ManagerService service) {
		this.service = service;
	}

	@RequestMapping("/list")
	@ResponseBody
	public Map<String,Map<String,String>> list() throws Exception{
		return service.list();
	}
	
	@RequestMapping("/detail")
	@ResponseBody
	public Map<String,String> detail(String name) throws Exception{
		Map<String,String> map = new HashMap<>();
		String detail = service.detail(name);
		detail = detail.replaceAll("\n", "<br>");
		map.put(name, detail);
		return map;
	}
	
	@RequestMapping("/xml")
	@ResponseBody
	public Map<String,String> viewXml(String name) throws Exception{ 
		Map<String,String> map = new HashMap<>();
		String xml = service.xml(name);
		map.put(name, xml);
		return map;
	}
	
	@RequestMapping("/apply")
	@ResponseBody
	public Map<String,String> apply(String name, String data) throws Exception{ 
		Map<String,String> map = new HashMap<>();
		map.put("msg", service.apply(name, data));
		return map;
	}
	
	@RequestMapping("/stop")
	@ResponseBody
	public Map<String,String> stop(String name) throws Exception{ 
		Map<String,String> map = new HashMap<>();
		map.put("msg", service.stop(name));
		return map;
	}
	
	@RequestMapping("/stopAll")
	@ResponseBody
	public Map<String,Object> stopAll() throws Exception{ 
		Map<String,Object> map = new HashMap<>();
		map.put("msg", service.stopAll());
		return map;
	}
	
	@RequestMapping("/start")
	@ResponseBody
	public Map<String,String> start(String name) throws Exception{ 
		Map<String,String> map = new HashMap<>();
		map.put("msg", service.start(name));
		return map;
	}
	
	@RequestMapping("/startAll")
	@ResponseBody
	public Map<String,Object> startAll() throws Exception{ 
		Map<String,Object> map = new HashMap<>();
		map.put("msg", service.startAll());
		return map;
	}
	
	@RequestMapping("/restart")
	@ResponseBody
	public Map<String,String> restart(String name) throws Exception{ 
		Map<String,String> map = new HashMap<>();
		map.put("msg", service.restart(name));
		return map;
	}
	
	@RequestMapping("/restartAll")
	@ResponseBody
	public Map<String,Object> restartAll() throws Exception{ 
		Map<String,Object> map = new HashMap<>();
		map.put("msg", service.restartAll());
		return map;
	}
}
