package org.eto.fom.boot.listener;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eto.fom.util.pool.PoolManager;

/**
 * 
 * @author shanhm
 *
 */
public class PoolListener implements ServletContextListener{

	private static Logger log;

	public PoolListener(){

	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		String logRoot = System.getProperty("log.root");
		if(StringUtils.isBlank(logRoot)){ 
			System.setProperty("log.root", System.getProperty("webapp.root") + File.separator + "log");
		}
		log = Logger.getLogger(PoolListener.class);
		ServletContext context = event.getServletContext();
		try{
			File poolXml = new File(context.getRealPath(context.getInitParameter("poolConfigLocation")));
			if(!poolXml.exists()){
				return;
			}
			PoolManager.listen(poolXml);
		}catch(Exception e){
			log.warn("pool init failed", e); 
			return;
		}
	}
}