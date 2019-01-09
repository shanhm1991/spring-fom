package com.fom.context.config;

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

import com.fom.context.exception.WarnException;
import com.fom.context.log.LoggerFactory;
import com.fom.util.IoUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
@Component(value="configLoader")
public class ConfigLoader extends AbstractRefreshableWebApplicationContext {
	
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
		
		loadApply();
		
		for(Config config : ConfigManager.getAll()){
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
			ConfigManager.register(config);
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
			LOG.info("\n"); 
			LOG.error(name + "加载异常", e);
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
			String location = Config.parseEnvValue(element.getTextTrim());
			//尝试读取绝对路径，如果不存在再以spring方式尝试
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
	
	private void loadApply() throws Exception{ 
		File apply = new File(System.getProperty("config.apply"));
		for(File file : apply.listFiles()){
			//自己缓存的文件目录不多做校验 
			if(!file.getName().contains(".xml.")){ 
				continue;
			}
			FileInputStream in = null;
			try{
				in = new FileInputStream(file);
				SAXReader reader = new SAXReader();
				reader.setEncoding("UTF-8");
				Document doc = reader.read(in); 
				Element element = doc.getRootElement();
				Config config = loadElement(element);
				if(config.valid){
					ConfigManager.register(config); 
				}
			}finally{
				IoUtil.close(in); 
			}
		}
	}

	public void writeApply(Config config, Document doc) throws Exception { 
		File apply = new File(System.getProperty("config.apply"));
		for(File file : apply.listFiles()){
			if(file.getName().startsWith(config.name + ".xml.") && !file.delete()){
				throw new WarnException("删除文件失败:" + file.getName());
			}
		}
		
		OutputFormat formater=OutputFormat.createPrettyPrint();  
		formater.setEncoding("UTF-8");  
		File xml = new File(apply + File.separator + config.name + ".xml." + config.loadTime);
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
			IoUtil.close(out); 
		}
		FileUtils.copyFile(xml, new File(apply + File.separator + "history" + File.separator + xml.getName()));
	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) 
			throws BeansException, IOException {
	}
}
