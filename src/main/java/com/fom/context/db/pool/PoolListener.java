package com.fom.context.db.pool;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.fom.context.ContextUtil;
import com.fom.context.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class PoolListener implements ServletContextListener{

	private static Logger log;
	
	public PoolListener(){
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		log = LoggerFactory.getLogger("pool");
		ServletContext context = event.getServletContext();
		try{
			File poolXml = new File(ContextUtil.getRealPath(context.getInitParameter("poolConfigLocation")));
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
}
