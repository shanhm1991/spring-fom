package org.eto.fom.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eto.fom.util.IoUtil;
import org.reflections.Reflections;

/**
 * context实例的管理
 * 
 * @author shanhm
 *
 */
public class ContextManager {

	private static final Logger LOG = Logger.getLogger(ContextManager.class);

	//Context构造器从中获取配置
	public static final Map<String, Element> elementMap = new ConcurrentHashMap<>();

	//Context构造器从中获取配置
	public static final Map<String, Map<String,String>> createMap = new ConcurrentHashMap<>();

	public static Map<String,Context> contextMap = new ConcurrentHashMap<>();

	public static boolean exist(String contextName){
		return contextMap.containsKey(contextName);
	}

	public static void register(Context context){
		if(context == null){
			return;
		}
		contextMap.put(context.name, context);
		LOG.info("regist context[" + context.name + "]");
	}

	public static void startAll(){
		for(Entry<String, Context> entry : contextMap.entrySet()){
			LOG.info("start context[" + entry.getKey() + "]");
			entry.getValue().startup();
		}

	}
	
	/**
	 * 加载缓存以及xmlPath配置文件中的context
	 * 
	 * @param xmlPath
	 */
	public static void load(String xmlPath){
		
		loadCacheContexts(); 
		
		File xml = new File(xmlPath);
		if(!xml.exists()){
			LOG.error("cann't find fom config file: " + xmlPath); 
			return;
		}else{
			LOG.info("load file: " + xmlPath); 
			try{
				SAXReader reader = new SAXReader();
				reader.setEncoding("UTF-8");
				Document doc = reader.read(new FileInputStream(xml));
				Element fom = doc.getRootElement();

				loadXmlContexts(fom);
				loadIncludeContexts(fom);
				loadAnnotationContexts(fom);
				ContextManager.startAll();
			}catch(Exception e){
				LOG.error("", e); 
				return;
			}
		}
	}
	
	private static void loadCacheContexts() {
		String cache = System.getProperty("cache.context");
		File[] array = new File(cache).listFiles();
		if(ArrayUtils.isEmpty(array)){
			return;
		}

		for(File file : array){
			if(file.isDirectory()){
				continue;
			}
			LOG.info("load cache:" + file.getName()); 
			String name = file.getName().split("\\.")[0];
			ObjectInputStream input = null;
			try{
				input = new ObjectInputStream(new FileInputStream(file));
				Context context = (Context) input.readObject();
				context.unSerialize();
				context.regist();
			}catch(Exception e){
				LOG.error("context[" + name + "] init failed", e);
			}finally{
				IoUtil.close(input);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static void loadXmlContexts(Element fom) {
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
				LOG.warn("invalid config[name=" + name + ",class=" + clazz + "]"); 
				continue;
			}
			try {
				Class<?> contextClass = Class.forName(clazz);
				if(!Context.class.isAssignableFrom(contextClass)){
					LOG.warn("ignore config, isn't subclass of com.fom.context.Context, [name=" 
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
					LOG.warn("context[" + name + "] already exist, init canceled.");
					continue;
				}

				ContextManager.elementMap.put(name, element);
				if(isNameEmpty){ 
					Context context = (Context)contextClass.newInstance();
					context.regist();
				}else{
					Constructor constructor = contextClass.getConstructor(String.class);
					Context context = (Context)constructor.newInstance(name);
					context.regist();
				}
			} catch (Exception e) {
				LOG.error("context[" + name + ",class=" + clazz + "] init failed", e);
			}
		}
	}
	
	private static void loadIncludeContexts(Element fom) {
		Element includes = fom.element("includes");
		if(includes == null){
			return;
		}
		
		String path = System.getProperty("webapp.root");
		Iterator<?> it = includes.elementIterator("include");
		while(it.hasNext()){
			Element element = (Element)it.next();
			String location = path + element.getTextTrim();
			File xml = new File(location);
			if(!xml.exists() || !xml.isFile()){
				LOG.warn("cann't find file: " + location);  
				continue;
			}
			LOG.info("load file: " + location);
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
	
	private static void loadAnnotationContexts(Element fom){
		Element scan = fom.element("fom-scan");
		if(scan == null){
			return;
		}
		String fomscan = scan.getTextTrim();
		if(StringUtils.isBlank(fomscan)){
			return;
		}

		LOG.info("load @FomContext from: " + fomscan);
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
				LOG.warn(clazz + " isn't subclass of com.fom.context.Context, ignored."); 
				continue;
			}

			String name = "";
			FomContext fc = clazz.getAnnotation(FomContext.class);
			if(fc != null && !StringUtils.isBlank(fc.name())){
				name = fc.name();
			}else{
				name = clazz.getSimpleName();
			}
			if(ContextManager.exist(name)){
				LOG.warn("context[" + name + "] already exist, init canceled.");
				continue;
			}

			try {
				Context context = (Context)clazz.newInstance();
				context.regist();
			} catch (Exception e) {
				LOG.error("context[" + clazz.getName() + "] init failed", e);
			} 
		}
	}
}
