package com.fom.context;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 
 * @author shanhm1991
 *
 */
public final class ConfigLoaderListener extends ContextLoaderListener {

	public ConfigLoaderListener(){

	}

	public ConfigLoaderListener(WebApplicationContext context) {
		super(context);
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event); 
		ServletContext context = event.getServletContext();
		ApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(context);
		ConfigLoader configloader = (ConfigLoader)springContext.getBean("configLoader");
		try{
			configloader.load(context, context.getInitParameter("fomConfigLocation"));
		}catch(Exception e){
			throw new RuntimeException("加载fom配置失败", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		super.contextDestroyed(event); 
	}
}
