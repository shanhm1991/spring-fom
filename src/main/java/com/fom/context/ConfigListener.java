package com.fom.context;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.fom.log.LoggerFactory;
import com.fom.util.IoUtil;

/**
 * 
 * @author shanhm
 *
 */
public class ConfigListener implements ServletContextListener {

	private static Logger log;

	private File fomXml;

	public ConfigListener(){

	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		log = LoggerFactory.getLogger("config");
		ServletContext context = event.getServletContext();
		setSystem(context);
		try {
			load(context.getInitParameter("fomConfigLocation"));
		} catch (Exception e) {
			log.error("config初始化失败", e);
		}
	}

	private void setSystem(ServletContext context){
		String cacheRoot = System.getProperty("cache.root");
		if(StringUtils.isBlank(cacheRoot)){
			cacheRoot = context.getRealPath("/WEB-INF/cache");
			if(StringUtils.isBlank(cacheRoot)){
				String root = System.getProperty("webapp.root");
				cacheRoot = root + File.separator + "cache";
			}
		}

		String path = cacheRoot + File.separator + "apply";
		File dir = new File(path + File.separator + "history");
		if(!dir.exists()){
			dir.mkdirs();
		}
		System.setProperty("config.apply", path);

		path = cacheRoot + File.separator + "iprogress";
		dir = new File(path);
		if(!dir.exists()){
			dir.mkdirs();
		}
		System.setProperty("import.progress", path);

		path = cacheRoot + File.separator + "dtemp";
		dir = new File(path);
		if(!dir.exists()){
			dir.mkdirs();
		}
		System.setProperty("download.temp", path);
	}

	private void load(String fomLocation) throws Exception {
		fomXml = new File(ContextUtil.getRealPath(fomLocation));
		SAXReader reader = new SAXReader();
		reader.setEncoding("UTF-8");

		Document doc = reader.read(new FileInputStream(fomXml));
		Element root = doc.getRootElement();
		loadElements(root);

		loadIncludes(root);

		loadApply();

		for(Config config : ConfigManager.getAll()){
			if(config.valid){
//				config.scanner.start();
				config.startTime = System.currentTimeMillis();
				config.isRunning = true;
			}
		}
	}

	private void loadElements(Element root) throws Exception {
		Iterator<?> it = root.elementIterator("fom");
		while(it.hasNext()){
			Config config = ConfigManager.load((Element)it.next());
			ConfigManager.register(config);
		}
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
			String location = ContextUtil.getEnvStr(element.getTextTrim());
			try{
				File xml = new File(fomPath + File.separator + location);
				//尝试读取相对路径，如果不存在再从springContext获取
				if(!xml.exists()){
					xml = new File(ContextUtil.getRealPath(location));
				}
				SAXReader reader = new SAXReader();
				reader.setEncoding("UTF-8");
				Document doc = reader.read(new FileInputStream(xml));
				loadElements(doc.getRootElement());
			}catch(Exception e){
				log.error("加载失败:" + location, e); 
			}

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
				Config config = ConfigManager.load(element);
				if(config.valid){
					ConfigManager.register(config); 
				}
			}finally{
				IoUtil.close(in); 
			}
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {

	}
}
