package com.fom.boot;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.fom.context.config.ConfigListener;
import com.fom.context.db.pool.PoolListener;

@Configuration
@ImportResource(locations= {"WEB-INF/springContext.xml"})
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
	
	
	
	
	

}
