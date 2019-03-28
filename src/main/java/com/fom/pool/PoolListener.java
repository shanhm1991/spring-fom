package com.fom.pool;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 
 * @author shanhm
 *
 */
public class PoolListener implements ServletContextListener{

	@Override
	public void contextInitialized(ServletContextEvent event) {
		PoolInitializer.set(event.getServletContext());
	}

}
