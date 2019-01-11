package com.fom.boot;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fom.context.config.ConfigListener;
import com.fom.context.db.pool.PoolListener;

@Configuration
public class ApplicationConfiguration extends WebMvcConfigurerAdapter {
	
	@Bean
	public ServletListenerRegistrationBean<ContextListener> listenContext(){
		System.out.println(1);
		ServletListenerRegistrationBean<ContextListener> listener = new ServletListenerRegistrationBean<>();
		listener.setListener(new ContextListener());
		listener.setOrder(3);
		return listener;
	}
	
	@Bean
	public ServletListenerRegistrationBean<PoolListener> listenPool(){
		System.out.println(2);
		ServletListenerRegistrationBean<PoolListener> listener = new ServletListenerRegistrationBean<>();
		listener.setListener(new PoolListener());
		listener.setOrder(4);
		return listener;
	}
	
	@Bean
	public ServletListenerRegistrationBean<ConfigListener> listenConfig(){
		System.out.println(3);
		ServletListenerRegistrationBean<ConfigListener> listener = new ServletListenerRegistrationBean<>();
		listener.setListener(new ConfigListener());
		listener.setOrder(5);
		return listener;
	}
	
	
	
	
	

}
