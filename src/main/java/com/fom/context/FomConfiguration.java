package com.fom.context;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.fom.pool.PoolListener;

/**
 * 启动参数:<br>
 * -Dwebapp.root="/"<br>
 * -Dcache.root="/WEB-INF/cache"<br>
 * -Dlog.root="/log"<br>
 * -Dlog4jConfigLocation="/WEB-INF/log4j.properties"<br>
 * -DfomConfigLocation="/WEB-INF/fom.xml"<br>
 * -DpoolConfigLocation="/WEB-INF/pool.xml"<br>
 * 
 * @author shanhm
 *
 */
@Configuration
@ImportResource(locations= {"**/*spring*.xml"})  
public class FomConfiguration implements ServletContextInitializer {
	@Bean
	public ServletListenerRegistrationBean<PoolListener> listenPool(){
		ServletListenerRegistrationBean<PoolListener> listener = new ServletListenerRegistrationBean<>();
		listener.setListener(new PoolListener());
		listener.setOrder(4);
		return listener;
	}

	@Bean
	public ServletListenerRegistrationBean<FomContextListener> listenConfig(){
		ServletListenerRegistrationBean<FomContextListener> listener = new ServletListenerRegistrationBean<>();
		listener.setListener(new FomContextListener());
		listener.setOrder(5);
		return listener;
	}

	@Bean
	public ServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {

			@Override
			protected void postProcessContext(Context context) {
				super.postProcessContext(context);

				String root = System.getProperty("webapp.root");
				if(StringUtils.isBlank(root)){
					root = ClassLoader.getSystemResource("").getPath();
					System.setProperty("webapp.root", root);
				}
				context.setDocBase(root); 

				String logRoot = System.getProperty("log.root");
				if(StringUtils.isBlank(logRoot)){ 
					System.setProperty("log.root", root + File.separator + "log");
				}

				String logPath = System.getProperty("log4jConfigLocation");
				if(StringUtils.isBlank(logPath)){
					PropertyConfigurator.configure(root + File.separator + "/WEB-INF/log4j.properties");
				}
			}
		};
		return tomcat;
	}

	@Override
	public void onStartup(ServletContext context) throws ServletException {
		String fomPath = System.getProperty("fomConfigLocation");
		if(StringUtils.isBlank(fomPath)){
			fomPath = "/WEB-INF/fom.xml";
		}
		String poolpath = System.getProperty("poolConfigLocation");
		if(StringUtils.isBlank(poolpath)){
			poolpath = "/WEB-INF/pool.xml";
		}
		context.setInitParameter("fomConfigLocation", fomPath);
		context.setInitParameter("poolConfigLocation", poolpath);	
		ContextUtil.setContext(context);
	}
}
