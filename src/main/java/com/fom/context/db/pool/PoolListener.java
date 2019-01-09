package com.fom.context.db.pool;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;

import com.fom.context.log.LoggerFactory;

public class PoolListener extends AbstractRefreshableWebApplicationContext implements ServletContextListener{

	private static Logger log;
	
	public PoolListener(){
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		log = LoggerFactory.getLogger("pool");
		ServletContext context = event.getServletContext();
		setServletContext(context); 
		try{
			File poolXml = getResource(context.getInitParameter("poolConfigLocation")).getFile();
			if(!poolXml.exists()){
				return;
			}
			PoolManager.listen(poolXml);
		}catch(Exception e){
			log.warn("pool初始化失败", e); 
			return;
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {

	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) 
			throws BeansException, IOException {
	}
}
