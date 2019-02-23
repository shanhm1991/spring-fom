package com.fom.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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
		case 1: return service.startup(name);
		case 2: return service.shutDown(name);
		case 3: return service.execNow(name);
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

	@RequestMapping("/listOtherLogs")
	@ResponseBody
	public Map<String, String> listOtherLogs() throws Exception {
		return service.listOtherLogs();
	}

	@RequestMapping("/queryLevel")
	@ResponseBody
	public Map<String, String> queryLevel(String logger) {
		Map<String, String> map = new HashMap<>();
		map.put("level", service.queryLevel(logger));
		return map;
	}

	@RequestMapping("/saveLevel")
	public String saveLevel(String logger, String level) {
		service.saveLevel(logger, level);
		return "success";
	}

	@RequestMapping("/successdetail")
	@ResponseBody
	public Map<String,Object> successDetail(String name) throws Exception { 
		return service.successDetail(name);
	}

	@RequestMapping("/faileddetail")
	@ResponseBody
	public Map<String,Object> failedDetail(String name) throws Exception { 
		return service.failedDetail(name);
	}

	@RequestMapping("/activedetail")
	@ResponseBody
	public Map<String,Object> activeDetail(String name) throws Exception{ 
		return service.activeDetail(name);
	}

	@RequestMapping("/waitingdetail")
	@ResponseBody
	public Map<String,Object> waitingdetail(String name) throws Exception { 
		return service.waitingdetail(name);
	}

	@RequestMapping("/saveCostLevel")
	@ResponseBody
	public Map<String,Object> saveCostLevel(String name, String levelStr, String saveDay, String date) throws Exception { 
		return service.saveCostLevel(name, levelStr, saveDay, date);
	}

	@RequestMapping("/changeDate")
	@ResponseBody
	public Map<String,Object> changeDate(String name, String date) throws Exception { 
		return service.changeDate(name, date);
	}

	@RequestMapping("/dataDownload")
	@ResponseBody
	public Map<String,Object> dataDownload(String name, HttpServletResponse resp) throws Exception{ 
		service.dataDownload(name, resp); 
		return null;
	}
}
