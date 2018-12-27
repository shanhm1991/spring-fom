package com.fom.context;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public final class ContextConfigListener extends ContextLoaderListener {

	public ContextConfigListener(){

	}

	public ContextConfigListener(WebApplicationContext context) {
		super(context);
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event); 
		ServletContext context = event.getServletContext();
		ApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(context);
		String root = System.getProperty("webapp.root");
		
		String path = root + File.separator + "cache" + File.separator + "apply";
		File cache = new File(path + File.separator + "history");
		if(!cache.exists()){
			cache.mkdirs();
		}
		System.setProperty("config.apply", path);
		
		path = root + File.separator + "cache" + File.separator + "iprogress";
		cache = new File(path);
		if(!cache.exists()){
			cache.mkdirs();
		}
		System.setProperty("import.progress", path);
		
		path = root + File.separator + "cache" + File.separator + "dtemp";
		cache = new File(path);
		if(!cache.exists()){
			cache.mkdirs();
		}
		System.setProperty("download.temp", path);
		
		ConfigLoader configloader = (ConfigLoader)springContext.getBean("configLoader");
		configloader.setServletContext(context); 
		try{
			configloader.load(context.getInitParameter("fomConfigLocation"));
		}catch(Exception e){
			throw new RuntimeException("加载fom配置失败", e);
		}
		
		PoolLoader poolLoader = (PoolLoader)springContext.getBean("poolLoader");
		poolLoader.load(context.getInitParameter("poolConfigLocation"));
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		super.contextDestroyed(event); 
	}
}
