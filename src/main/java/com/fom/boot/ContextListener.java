package com.fom.boot;

import java.io.File;
import java.io.FileNotFoundException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.util.ResourceUtils;

import com.fom.context.ContextUtil;

public class ContextListener implements ServletContextListener {
	
	private static String ss;
	
	public ContextListener(){

	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext context = event.getServletContext();
		
//		context.setInitParameter("fomConfigLocation", "/WEB-INF/fom.xml");
//		context.setInitParameter("poolConfigLocation", "/WEB-INF/pool.xml");
		
		try {
			ss = ResourceUtils.getURL("classpath:").getPath();
			System.out.println(ss);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(new File("").getAbsolutePath()); 

		ContextUtil.INSTANCE.setServletContext(context);
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {

	}
}
