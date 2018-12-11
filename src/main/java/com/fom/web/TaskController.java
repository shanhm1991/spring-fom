package com.fom.web;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fom.util.log.LoggerFactory;

@Controller
public class TaskController {

	
	private static Logger log = LoggerFactory.getLogger("web"); 

	@RequestMapping("/hellofm")
	public String hello(){
		System.out.println("hello world");

		log.info("sadassfdsgfdsgsiufenveiunknjbjf");
		return "monitor";
	}
}
