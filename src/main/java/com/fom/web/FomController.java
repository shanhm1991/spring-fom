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
	public Map<String, Object> list() throws Exception{
		return service.list();
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
	
}
