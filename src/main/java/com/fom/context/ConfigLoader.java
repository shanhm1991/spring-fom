package com.fom.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;

import com.fom.util.log.LoggerFactory;

/**
 * 
 * @author shanhm1991
 *
 */
@Component(value="configLoader")
class ConfigLoader extends AbstractRefreshableWebApplicationContext {

	private static final Logger LOG = LoggerFactory.getLogger("config");

	void load(ServletContext context, String configLocation) throws Exception{
		this.setConfigLocation(configLocation);
		this.setServletContext(context); 

		File fomXml = this.getResource(getConfigLocations()[0]).getFile();
		SAXReader reader = new SAXReader();
		reader.setEncoding("UTF-8");
		Document doc = reader.read(new FileInputStream(fomXml));

		Element operators = doc.getRootElement();
		Iterator<?> it = operators.elementIterator("fom");
		while(it.hasNext()){
			Element element = (Element)it.next();
			String name = element.attributeValue("name");
			String clzz = element.attributeValue("config");
			Class<?> configClass = Class.forName(clzz);
			Constructor<?> constructor = configClass.getDeclaredConstructor(String.class); 
			constructor.setAccessible(true); 
			Config config = (Config)constructor.newInstance(name);
			config.element = element;
			config.loader = this;

			config.load();
			config.valid();
			ConfigManager.registerConfig(config);
			LOG.info("#加载配置: " + name + config);
		}
		
		for(Config config : ConfigManager.getAllConfig()){
			config.scanner.start();
		}
	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) 
			throws BeansException, IOException {
	}
}
