package com.fom.context;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;

import com.fom.util.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
@Component(value="poolLoader")
public class PoolLoader extends AbstractRefreshableWebApplicationContext {
	
	private static final Logger LOG = LoggerFactory.getLogger("pool");

	void load(String poolLocation) {
		try{
			File poolXml = getResource(poolLocation).getFile();
			ManagerServiceImpl.listen(poolXml);
		}catch(Exception e){
			LOG.warn("pool初始化失败", e); 
			return;
		}
	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) 
			throws BeansException, IOException {
		
	}

}
