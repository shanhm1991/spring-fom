package com.fom.boot;

import org.apache.catalina.Context;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.fom.context.config.ConfigListener;
import com.fom.db.pool.PoolListener;

/**
 * 
 * @author shanhm
 * @date 2019年1月14日
 *
 */
@Configuration
@ImportResource(locations= {"**/*spring*.xml"})  
public class ApplicationConfiguration {
	@Bean
	public ServletListenerRegistrationBean<PoolListener> listenPool(){
		ServletListenerRegistrationBean<PoolListener> listener = new ServletListenerRegistrationBean<>();
		listener.setListener(new PoolListener());
		listener.setOrder(4);
		return listener;
	}
	
	@Bean
	public ServletListenerRegistrationBean<ConfigListener> listenConfig(){
		ServletListenerRegistrationBean<ConfigListener> listener = new ServletListenerRegistrationBean<>();
		listener.setListener(new ConfigListener());
		listener.setOrder(5);
		return listener;
	}
	
	@Bean
	public ServletWebServerFactory servletContainer() {
	    TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
	        @Override
	        protected void postProcessContext(Context context) {
	        	super.postProcessContext(context);
	        	context.setDocBase(System.getProperty("webapp.root")); 
	        }
	    };
	    return tomcat;
	}
}
