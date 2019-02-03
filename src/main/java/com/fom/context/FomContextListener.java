package com.fom.context;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
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

	private static final Logger LOG = Logger.getRootLogger();

	public FomContextListener(){

	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext servlet = event.getServletContext(); 
		setSystem(servlet);

		loadCacheContexts(); 

		String file = ContextUtil.getContextPath(servlet.getInitParameter("fomConfigLocation"));
		File xml = new File(file);
		if(!xml.exists()){
			LOG.error("没有找到fom配置:" + file); 
			return;
		}
		LOG.info("加载:" + file); 
		Element fom = null;
		try{
			SAXReader reader = new SAXReader();
			reader.setEncoding("UTF-8");
			Document doc = reader.read(new FileInputStream(xml));
			fom = doc.getRootElement();
		}catch(Exception e){
			LOG.error("", e); 
			return;
		}
		loadXmlContexts(fom);
		loadIncludeContexts(fom);
		loadAnnotationContexts(fom);
		ContextManager.startAll();
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

	private void loadCacheContexts() {

	}

	@SuppressWarnings("rawtypes")
	private void loadXmlContexts(Element fom) {
		Element contexts = fom.element("contexts");
		if(contexts == null){
			return;
		}
		Iterator<?> it = contexts.elementIterator("context");
		while(it.hasNext()){
			Element element = (Element)it.next();
			String name = element.attributeValue("name");
			String clazz = element.attributeValue("class");
			if(StringUtils.isBlank(clazz)){
				LOG.warn("非法配置:[name=" + name + ",class=" + clazz + "]"); 
				continue;
			}
			try {
				Class<?> contextClass = Class.forName(clazz);
				if(!Context.class.isAssignableFrom(contextClass)){
					LOG.warn("忽略配置,没有继承com.fom.context.Context, [name=" 
							+ name + ",class=" + clazz + "]");
					continue;
				}
				boolean isNameEmpty = false; 
				if(StringUtils.isBlank(name)){ 
					isNameEmpty = true;
					FomContext fc = contextClass.getAnnotation(FomContext.class);
					if(fc != null && !StringUtils.isBlank(fc.name())){
						name = fc.name();
					}else{
						name = contextClass.getSimpleName();
					}
				}
				if(ContextManager.exist(name)){
					LOG.warn("context[" + name + "]已经存在,取消加载.");
					continue;
				}
				
				ContextManager.elementMap.put(name, element);
				Context context = null;
				if(isNameEmpty){ 
					context = (Context)contextClass.newInstance();
				}else{
					Constructor constructor = contextClass.getConstructor(String.class);
					context = (Context)constructor.newInstance(name);
				}
				ContextManager.register(context); 
			} catch (Exception e) {
				LOG.error("context[" + name + ",class=" + clazz + "]初始化异常", e);
			}
		}
	}

	private void loadIncludeContexts(Element fom) {
		Element includes = fom.element("includes");
		if(includes == null){
			return;
		}
		Iterator<?> it = includes.elementIterator("include");
		while(it.hasNext()){
			Element element = (Element)it.next();
			String location = ContextUtil.getContextPath(element.getTextTrim());
			File xml = new File(location);
			if(!xml.exists()){
				LOG.warn("没有找到配置:" + location);  
				continue;
			}
			LOG.info("加载配置:" + location);
			try{
				SAXReader reader = new SAXReader();
				reader.setEncoding("UTF-8");
				Document doc = reader.read(new FileInputStream(xml));
				Element root = doc.getRootElement();
				loadXmlContexts(root);
			}catch(Exception e){
				LOG.error("", e); 
			}
		}
	}

	private void loadAnnotationContexts(Element fom){
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
		
		Set<Class<?>> contextSet = new HashSet<>();
		for(String pack : pckages){
			Reflections reflections = new Reflections(pack.trim());
			contextSet.addAll(reflections.getTypesAnnotatedWith(FomContext.class));
		}
		for(Class<?> clazz : contextSet){
			if(!Context.class.isAssignableFrom(clazz)){
				LOG.warn(clazz + "没有继承com.fom.context.Context, 忽略@FomContext"); 
				continue;
			}
			try {
				Context context = (Context)clazz.newInstance();
				ContextManager.register(context); 
			} catch (Exception e) {
				LOG.error("context[" + clazz.getName() + "]初始化异常", e);
			} 
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {

	}
}
