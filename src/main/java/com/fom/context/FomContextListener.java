package com.fom.context;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.reflections.Reflections;

import com.fom.log.LoggerFactory;
import com.fom.util.IoUtil;

/**
 * 初始化容器，加载配置文件fom.xml
 * <br>&lt;fom&gt;
 * <br>&nbsp;&nbsp;&lt;fom-scan/&gt;
 * <br>&nbsp;&nbsp;&lt;includes&gt;
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;include/&gt;
 * <br>&nbsp;&nbsp;&lt;/includes&gt;
 * <br>&nbsp;&nbsp;&lt;contexts&gt;
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;context/&gt;
 * <br>&nbsp;&nbsp;&lt;/contexts&gt;
 * <br>&nbsp;&nbsp;&lt;configs&gt;
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;config/&gt;
 * <br>&nbsp;&nbsp;&lt;/configs&gt;
 * <br>&lt;/fom&gt;
 * <br>加载策略:
 * <br>1.加载cache中所有缓存的所有config
 * <br>2.加载cache中所有缓存的所有context
 * <br>3.加载主配置文件中的configs节点
 * <br>4.加载主配置文件中的contexts节点
 * <br>5.依次加载includes节点下包含的配置文件中的configs和contexts节点
 * <br>6.扫描fom-scan节点配置所有包package下面的注解类
 * <br>当出现name同名时，只取第一个加载成功的，丢弃后续的，当加载完毕后启动容器中所有的context
 * 
 * @author shanhm
 *
 */
public class FomContextListener implements ServletContextListener {

	private static Logger log;

	public FomContextListener(){

	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		log = LoggerFactory.getLogger("context");
		ServletContext servlet = event.getServletContext(); 
		setSystem(servlet);

		loadCacheConfigs();

		loadCacheContexts(); 

		String file = ContextManager.getContextPath(servlet.getInitParameter("fomConfigLocation"));
		File xml = new File(file);
		if(!xml.exists()){
			log.error("没有找到fom配置:" + file); 
			return;
		}

		log.info("加载:" + file); 
		Element fom = null;
		try{
			SAXReader reader = new SAXReader();
			reader.setEncoding("UTF-8");
			Document doc = reader.read(new FileInputStream(xml));
			fom = doc.getRootElement();
		}catch(Exception e){
			log.error("", e); 
			return;
		}

		loadConfigsElement(fom);

		loadContextsElement(fom);

		loadIncludesElement(fom);

		loadFomScanElement(fom);
	} 

	private void setSystem(ServletContext servlet){
		String cacheRoot = System.getProperty("cache.root");
		if(StringUtils.isBlank(cacheRoot)){
			cacheRoot = servlet.getRealPath("/WEB-INF/cache");
			if(StringUtils.isBlank(cacheRoot)){
				String root = System.getProperty("webapp.root");
				cacheRoot = root + File.separator + "cache";
			}  
			System.setProperty("cache.root", cacheRoot);
		}
		String configCache = System.getProperty("cache.config");
		if(StringUtils.isBlank(configCache)){
			configCache = cacheRoot + File.separator + "config";
			File history = new File(configCache + File.separator + "history");
			if(!history.exists()){
				history.mkdirs();
			}
			System.setProperty("cache.config", configCache);
		}
		String parseCache = System.getProperty("cache.parse");
		if(StringUtils.isBlank(parseCache)){
			parseCache = cacheRoot + File.separator + "parse";
			File parse = new File(parseCache);
			if(!parse.exists()){
				parse.mkdirs();
			}
			System.setProperty("cache.parse", parseCache);
		}
		String downloadCache = System.getProperty("cache.download");
		if(StringUtils.isBlank(downloadCache)){
			downloadCache = cacheRoot + File.separator + "download";
			File download = new File(downloadCache);
			if(!download.exists()){
				download.mkdirs();
			}
			System.setProperty("cache.download", downloadCache);
		}
	}

	private void loadCacheConfigs() { 
		File cache = new File(System.getProperty("cache.config"));
		if(!cache.exists()){
			return;
		}
		File[] array = cache.listFiles();
		if(ArrayUtils.isEmpty(array)){
			return;
		}
		for(File file : cache.listFiles()){
			if(file.isDirectory()){
				continue;
			}
			log.info("加载缓存:" + file.getPath()); 
			FileInputStream in = null;
			try{
				in = new FileInputStream(file);
				SAXReader reader = new SAXReader(); 
				reader.setEncoding("UTF-8");
				Document doc = reader.read(in); 
				Element element = doc.getRootElement();
				Config config = ConfigManager.load(element);
				ConfigManager.register(config); 
			}catch(Exception e){
				log.error("", e); 
			}finally{
				IoUtil.close(in); 
			}
		}
	}

	//TODO
	private void loadCacheContexts() {

	}

	private void loadConfigsElement(Element fom){
		Element configs = fom.element("configs");
		if(configs == null){
			return;
		}
		Iterator<?> it = configs.elementIterator("config");
		while(it.hasNext()){
			Config config = ConfigManager.load((Element)it.next());
			ConfigManager.register(config); 
		}
	}

	private void loadContextsElement(Element fom) {
		Element contexts = fom.element("contexts");
		if(contexts == null){
			return;
		}
		Iterator<?> it = contexts.elementIterator("context");
		while(it.hasNext()){
			Element element = (Element)it.next();
			String name = element.attributeValue("name");
			String clzz = element.attributeValue("class");
			if(StringUtils.isBlank(name) || StringUtils.isBlank(clzz)){
				log.warn("非法context配置:" + name + "=" + clzz); 
				continue;
			}
			try {
				Class<?> contextClass = Class.forName(clzz);
				if(!Context.class.isAssignableFrom(contextClass)){
					log.warn("非法context配置[没有继承com.fom.context.Context]:" + name + "=" + clzz); 
					continue;
				}
				Context context = (Context)contextClass.newInstance();
				context.name = name;

				String remark = "";
				Element rm = (Element)element.element("remark");
				if(rm != null){
					remark = rm.getTextTrim();
				}
				ContextManager.register(context);
			} catch (Exception e) {
				log.error("[" + name + "]context初始化异常", e);
			}
		}
	}

	private void loadIncludesElement(Element fom) {
		Element includes = fom.element("includes");
		if(includes == null){
			return;
		}
		Iterator<?> it = includes.elementIterator("include");
		while(it.hasNext()){
			Element element = (Element)it.next();
			String location = ContextManager.getContextPath(element.getTextTrim());
			File xml = new File(location);
			if(!xml.exists()){
				log.warn("没有找到配置:" + location);  
				continue;
			}
			log.info("加载配置:" + location);
			try{
				SAXReader reader = new SAXReader();
				reader.setEncoding("UTF-8");
				Document doc = reader.read(new FileInputStream(xml));
				Element root = doc.getRootElement();
				loadConfigsElement(root);
				loadContextsElement(root);
			}catch(Exception e){
				log.error("", e); 
			}
		}
	}

	private void loadFomScanElement(Element fom) {
		Element scan = fom.element("fom-scan");
		if(scan == null){
			return;
		}
		String fomscan = scan.getTextTrim();
		if(StringUtils.isBlank(fomscan)){
			return;
		}
		String[] pckages = fomscan.split(",");
		if(ArrayUtils.isEmpty(pckages)){
			return;
		}

		loadAnnotationConfigs(pckages);

		loadAnnotationContexts(pckages);
	}

	//TODO
	private void loadAnnotationConfigs(String[] pckages){

	}

	private void loadAnnotationContexts(String[] pckages){
		Set<Class<?>> contextSet = new HashSet<>();
		for(String pack : pckages){
			Reflections reflections = new Reflections(pack.trim());
			contextSet.addAll(reflections.getTypesAnnotatedWith(FomContext.class));
		}
		for(Class<?> clazz : contextSet){
			if(!Context.class.isAssignableFrom(clazz)){
				log.warn(clazz + "没有继承com.fom.context.Context, 忽略无效"); 
				continue;
			}
			FomContext fc = clazz.getAnnotation(FomContext.class);
			String[] names = fc.names();
			for(String name : names){
				try {
					Context context = (Context)clazz.newInstance();
					context.name = name;
					ContextManager.register(context);
				} catch (Exception e) {
					log.error("[" + clazz + "]context初始化异常", e);
					break;
				} 
			}
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {

	}
}
