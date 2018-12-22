package com.fom.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fom.context.ManagerService;
import com.fom.util.IoUtils;

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
	
	@RequestMapping("/logs")
	@ResponseBody
	public Map<String,Object> logs() throws Exception{ 
		Map<String,Object> map = new HashMap<>();
		String path = System.getProperty("webapp.root");
		File logs = new File(path + File.separator + "log");
		String[] array = logs.list();
		if(array != null){
			map.put("logs", Arrays.asList(array));
		}
		return map;
	}
	
	@RequestMapping("/download")
	@ResponseBody
	public Map<String,Object> download(String file, HttpServletResponse resp) throws Exception{ 
		String path = System.getProperty("webapp.root") + File.separator + "log";
		File log = new File(path + File.separator + file);
		resp.reset();
		resp.setContentType("application/octet-stream;charset=UTF-8");
		resp.addHeader("Content-Disposition", "attachment;filename=\"" + file +"\""); 
		InputStream in = null;
		int len = 0;
		byte[] bytes =  new byte[1024];
		try{
			in = new FileInputStream(log);
			while((len = in.read(bytes)) > 0){
				resp.getOutputStream().write(bytes, 0, len);
			}
		}finally{
			IoUtils.close(in);
		}
		return null;
	}
	
	@RequestMapping("/srcs")
	@ResponseBody
	public Map<String,Object> srcs(String name, boolean match) throws Exception{ 
		return service.srcs(name, match);
	}
}
