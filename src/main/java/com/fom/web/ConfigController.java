package com.fom.web;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fom.context.Config;
import com.fom.context.ManagerService;
import com.fom.util.log.LoggerFactory;

@Controller
public class ConfigController {

	private static Logger log = LoggerFactory.getLogger("web"); 

	@Autowired
	@Qualifier("managerService")
	private ManagerService service;

	public void setService(ManagerService service) {
		this.service = service;
	}

	@RequestMapping("/hellofm")
	@ResponseBody
	public Map<String,Config> hello() throws IOException{
		log.info("receive request of config manage"); 
		return service.getConfigMap();
	}
}
