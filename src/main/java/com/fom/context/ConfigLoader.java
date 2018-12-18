package com.fom.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;

import com.fom.util.IoUtils;
import com.fom.util.exception.WarnException;
import com.fom.util.log.LoggerFactory;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
@Component(value="configLoader")
class ConfigLoader extends AbstractRefreshableWebApplicationContext {

	private static final Logger LOG = LoggerFactory.getLogger("config");

	private File fomXml;

	void load(String fomLocation) throws Exception {
		fomXml = getResource(fomLocation).getFile();
		SAXReader reader = new SAXReader();
		reader.setEncoding("UTF-8");
		Document doc = reader.read(new FileInputStream(fomXml));
		Element root = doc.getRootElement();
		loadElements(root);
		loadIncludes(root);
		for(Config config : ConfigManager.getAllConfig()){
			if(config.valid){
				config.scanner.start();
				config.startTime = System.currentTimeMillis();
				config.isRunning = true;
			}
		}
	}

	private void loadElements(Element root) throws Exception {
		Iterator<?> it = root.elementIterator("fom");
		while(it.hasNext()){
			Config config = loadElement((Element)it.next());
			ConfigManager.registerConfig(config);
			LOG.info("#加载配置: " + config.name + config);
		}
	}
	
	public Config loadElement(Element element) {
		String name = element.attributeValue("name");
		String clzz = element.attributeValue("config");
		Config config = null;
		try{
			Class<?> configClass = Class.forName(clzz);
			Constructor<?> constructor = configClass.getDeclaredConstructor(String.class); 
			constructor.setAccessible(true); 
			config = (Config)constructor.newInstance(name);
			config.loadTime = System.currentTimeMillis();
			config.element = element;
			config.loader = this;
			config.load();
			config.valid = config.valid();
		}catch(Exception e){
			if(config != null){
				config.valid = false;
			}
		}
		return config;
	}

	private void loadIncludes(Element root) throws Exception {
		Element includes = root.element("includes");
		if(includes == null){
			return;
		}
		Iterator<?> it = includes.elementIterator("include");
		String fomPath = fomXml.getParent();
		while(it.hasNext()){
			Element element = (Element)it.next();
			String location = element.getTextTrim();
			//相对路径找不到再找绝对路径
			File xml = new File(fomPath + File.separator + location);
			if(!xml.exists()){
				xml = getResource(location).getFile();
			}
			SAXReader reader = new SAXReader();
			reader.setEncoding("UTF-8");
			Document doc = reader.read(new FileInputStream(xml));
			loadElements(doc.getRootElement());
		}
	}

	public void writeApply(Config config, Document doc) throws Exception { 
		File updatePath = getResource("/WEB-INF/update").getFile();
		for(File file : updatePath.listFiles()){
			if(file.getName().startsWith(config.name + ".xml.") && !file.delete()){
				throw new WarnException("删除文件失败:" + file.getName());
			}
		}
		
		OutputFormat formater=OutputFormat.createPrettyPrint();  
		formater.setEncoding("UTF-8");  
		File xml = new File(updatePath + File.separator + config.name + ".xml." + config.loadTime);
		FileOutputStream out = null;
		XMLWriter writer = null;
		try{
			out = new FileOutputStream(xml);
			writer=new XMLWriter(out,formater);
			writer.setEscapeText(false);
			writer.write(doc);  
			writer.flush();
			writer.close();
		}finally{
			IoUtils.close(out); 
		}
		
		File historyPath = getResource("/WEB-INF/update/history").getFile();
		FileUtils.copyFile(xml, new File(historyPath + File.separator + xml.getName()));
	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) 
			throws BeansException, IOException {
	}
}
