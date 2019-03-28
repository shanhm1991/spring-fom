package com.fom.pool;

import java.io.File;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.fom.context.ContextUtil;

/**
 * 
 * @author shanhm
 *
 */
@Component
class PoolInitializer implements ApplicationRunner {

	private static final Logger LOG = Logger.getLogger(PoolInitializer.class);

	//volatile只能保证引用的变化立即刷新，但系统对这个引用只有一次引用赋值操作
	private static volatile ServletContext servlet;

	static void set(ServletContext context){
		if(servlet != null){
			throw new UnsupportedOperationException();
		}
		servlet = context;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String logRoot = System.getProperty("log.root");
		if(StringUtils.isBlank(logRoot)){ 
			System.setProperty("log.root", System.getProperty("webapp.root") + File.separator + "log");
		}

		try{
			File poolXml = new File(ContextUtil.getContextPath(servlet.getInitParameter("poolConfigLocation")));
			if(!poolXml.exists()){
				return;
			}
			PoolManager.listen(poolXml);
		}catch(Exception e){
			LOG.warn("pool init failed", e); 
		}
	}

}
