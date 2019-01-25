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
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fom.context.FomService;
import com.fom.util.IoUtil;

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
	public Map<String,Map<String,String>> list() throws Exception{
		return service.list();
	}
	
	@RequestMapping("/detail")
	@ResponseBody
	public Map<String,String> detail(String name) throws Exception{
		Map<String,String> map = new HashMap<>();
		map.put(name, service.detail(name));
		return map;
	}
	
	@RequestMapping("/xml")
	@ResponseBody
	public Map<String,String> viewXml(String name) throws Exception{ 
		Map<String,String> map = new HashMap<>();
		map.put(name, service.xml(name));
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
		File logs = new File(System.getProperty("log.root"));
		String[] array = logs.list();
		if(array != null){
			map.put("logs", Arrays.asList(array));
		}
		return map;
	}
	
	@RequestMapping("/download")
	@ResponseBody
	public Map<String,Object> download(String file, HttpServletResponse resp) throws Exception{ 
		String path = System.getProperty("log.root") + File.separator + file;
		File log = new File(path);
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
			IoUtil.close(in);
		}
		return null;
	}
	
	public static void main(String[] args) {
        SpringApplication.run(FomController.class, args); 
    }
}
