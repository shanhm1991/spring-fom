package org.eto.fom.context.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eto.fom.context.SpringContext;
import org.eto.fom.context.SpringRegistry;
import org.eto.fom.context.annotation.FomConfig;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.context.annotation.FomSchedulBatch;
import org.eto.fom.context.annotation.SchedulBatchFactory;
import org.eto.fom.util.IoUtil;
import org.reflections.Reflections;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * context实例的管理
 * 
 * @author shanhm
 *
 */
public class ContextManager {

	private static final Logger LOG = Logger.getLogger(ContextManager.class);

	public static final String LOADFROM_CACHE = "cache";

	public static final String LOADFROM_XML = "xml";

	public static final String LOADFROM_FOMCONTEXT = "@FomContext";

	public static final String LOADFROM_FOMSCHEDUL = "@FomSchedul";

	public static final String LOADFROM_FOMSCHEDULBATCH = "@FomSchedulBatch";

	public static final String LOADFROM_INPUT = "input";
	
	private static ResourcePatternResolver pathResolver = new PathMatchingResourcePatternResolver();

	//Context构造器从中获取配置
	public static final Map<String, Element> elementMap = new ConcurrentHashMap<>();

	//Context构造器从中获取配置
	public static final Map<String, Map<String,String>> createMap = new ConcurrentHashMap<>();

	public static ConcurrentMap<String,Context> contextMap = new ConcurrentHashMap<>();

	public static boolean exist(String contextName){
		return contextMap.containsKey(contextName);
	}

	public static void register(Context context, String loadFrom) throws Exception {
		if(context == null){
			return;
		}

		Context exits = contextMap.putIfAbsent(context.name, context);
		if(exits != null){
			LOG.warn("context[" + context.name + "] already exist, load ignored.");
			return;
		}

		SpringRegistry registry = SpringContext.getBean(SpringRegistry.class);
		registry.regist(context.name, context); 
		
		Class<?> clazz = context.getClass();
		//扫描FomContfig
		Field[] fields = clazz.getDeclaredFields();
		for(Field f : fields){
			FomConfig c = f.getAnnotation(FomConfig.class);
			if(c != null){
				String value = c.value();
				if(value.indexOf("${") != -1){ 
					value = SpringContext.getPropertiesValue(value);
				}
				
				f.setAccessible(true); 
				f.set(context, value);
				
				String key = c.key();
				if(StringUtils.isBlank(key)){
					key = f.getName();
				}
				context.config.put(key, value); 
			}
		}

		//扫描PostConstruct
		for(Method method : clazz.getMethods()){
			PostConstruct con = method.getAnnotation(PostConstruct.class);
			if(con != null){
				method.invoke(context);
			}
		}

		LOG.info("load context[" + context.name + "] from " + loadFrom + ", " + context.config);
	}

	public static void startAll(){
		for(Entry<String, Context> entry : contextMap.entrySet()){
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
			LOG.info("cann't find fom config: " + xmlPath); 
			return;
		}else{
			LOG.info("load fom config: " + xmlPath); 
			try{
				SAXReader reader = new SAXReader();
				reader.setEncoding("UTF-8");
				Document doc = reader.read(new FileInputStream(xml));
				Element fom = doc.getRootElement();

				loadXmlContexts(fom, xmlPath);  

				loadxmlIncludes(fom, xmlPath);

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

				loadFomContexts(pckages);

				loadFomSchedul(pckages);
				
				loadFomSchedulBatch(pckages);

				startAll();
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

			String name = file.getName().split("\\.")[0];
			ObjectInputStream input = null;
			try{
				input = new ObjectInputStream(new FileInputStream(file));
				Context context = (Context) input.readObject();
				context.unSerialize();
				context.regist(LOADFROM_CACHE);
			}catch(Exception e){
				LOG.error("load context[" + name + "] from cache failed", e);
			}finally{
				IoUtil.close(input);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static void loadXmlContexts(Element fom, String xmlPath) {
		Element contexts = fom.element("contexts");
		if(contexts == null){
			return;
		}

		LOG.info("load context from: " + xmlPath); 
		Iterator<?> it = contexts.elementIterator("context");
		while(it.hasNext()){
			Element element = (Element)it.next();
			String name = element.attributeValue("name");
			String clazz = element.attributeValue("class");
			if(StringUtils.isBlank(clazz)){
				LOG.warn("ignore context[" + name + "] from xml, class can not be empty."); 
				continue;
			}

			try {
				Class<?> contextClass = Class.forName(clazz);
				if(!Context.class.isAssignableFrom(contextClass)){
					LOG.warn("ignore context[" + name + "] from xml, " + clazz + " isn't a Context."); 
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

				ContextManager.elementMap.put(name, element);
				if(isNameEmpty){ 
					Context context = (Context)contextClass.newInstance();
					context.regist(LOADFROM_XML);
				}else{
					Constructor constructor = contextClass.getConstructor(String.class);
					Context context = (Context)constructor.newInstance(name);
					context.regist(LOADFROM_XML);
				}
			} catch (Exception e) {
				LOG.error("load context[" + name + "] from xml failed", e);
			}
		}
	}

	private static void loadxmlIncludes(Element fom, String xmlPath) throws IOException {
		Element includes = fom.element("includes");
		if(includes == null){
			return;
		}

//		String path = System.getProperty("webapp.root");
		Iterator<?> it = includes.elementIterator("include");
		while(it.hasNext()){
			Element element = (Element)it.next();
			String location = xmlPath + File.separator + element.getTextTrim();
			
			System.out.println(Arrays.asList(location)); 
			Resource[] arr = pathResolver.getResources(location);
			System.out.println(Arrays.asList(arr)); 
			
			File xml = new File(location);
			if(!xml.exists() || !xml.isFile()){
				LOG.warn("cann't find config: " + location);  
				continue;
			}

			try{
				SAXReader reader = new SAXReader();
				reader.setEncoding("UTF-8");
				Document doc = reader.read(new FileInputStream(xml));
				Element root = doc.getRootElement();
				loadXmlContexts(root, location);
			}catch(Exception e){
				LOG.error("", e); 
			}
		}
	}

	private static void loadFomContexts(String[] pckages){
		Set<Class<?>> clazzs = new HashSet<>();
		for(String pack : pckages){
			Reflections reflections = new Reflections(pack.trim());
			clazzs.addAll(reflections.getTypesAnnotatedWith(FomContext.class));
		}

		for(Class<?> clazz : clazzs){
			if(!Context.class.isAssignableFrom(clazz)){
				LOG.warn("ignore context with @FomContext, " + clazz + " isn't a Context");
				continue;
			}

			try {
				Context context = (Context)clazz.newInstance();
				context.regist(LOADFROM_FOMCONTEXT);
			} catch (Exception e) {
				LOG.error("load context from @FomContext failed", e);
			} 
		}
	}

	@SuppressWarnings({ "serial" })
	private static void loadFomSchedul(String[] pckages) throws Exception {
		Set<Class<?>> clazzs = new HashSet<>();
		for(String pack : pckages){
			Reflections reflections = new Reflections(pack.trim());
			clazzs.addAll(reflections.getTypesAnnotatedWith(FomSchedul.class));
		}

		SpringRegistry registry = SpringContext.getBean(SpringRegistry.class);
		for(Class<?> clazz : clazzs){
			FomSchedul fom = clazz.getAnnotation(FomSchedul.class);

			String name = fom.name();
			if(StringUtils.isBlank(name)){
				name = clazz.getSimpleName();
			}

			final Object instance = clazz.newInstance();
			registry.regist(name + "-task", instance); 

			String cron = fom.cron();
			//没有@Scheduled则忽略
			List<Method> methods = new ArrayList<>();
			for(Method method : clazz.getMethods()){
				Scheduled sch = method.getAnnotation(Scheduled.class);
				if(sch != null){
					methods.add(method);
					if(StringUtils.isBlank(cron)){
						cron = sch.cron();
					}
				}
			}

			if(methods.isEmpty()){
				LOG.warn("ignore context with @FomSchedul, " + clazz + " hasn't Scheduled method");
				continue;
			}

			if(StringUtils.isBlank(cron)){
				LOG.warn("ignore context with @FomSchedul, " + clazz + " hasn't cron expression.");
				continue;
			}

			Map<String, String> map = new HashMap<>();
			map.put(ContextConfig.CONF_CRON, cron);
			map.put(ContextConfig.CONF_THREADCORE, String.valueOf(fom.threadCore()));
			map.put(ContextConfig.CONF_THREADMAX, String.valueOf(fom.threadMax()));
			map.put(ContextConfig.CONF_QUEUESIZE, String.valueOf(fom.queueSize()));
			map.put(ContextConfig.CONF_OVERTIME, String.valueOf(fom.threadOverTime()));
			map.put(ContextConfig.CONF_ALIVETIME, String.valueOf(fom.threadAliveTime()));
			map.put(ContextConfig.CONF_CANCELLABLE, String.valueOf(fom.cancellable()));
			map.put(ContextConfig.CONF_EXECONLOAN, String.valueOf(fom.execOnLoad()));
			map.put(ContextConfig.CONF_STOPWITHNOCRON, String.valueOf(fom.stopWithNoCron()));
			map.put(ContextConfig.CONF_REMARK, String.valueOf(fom.remark()));

			createMap.putIfAbsent(name, map);

			Context context = new Context(name){

				private final Object target = instance;

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				protected Collection<Task> scheduleBatch() throws Exception {
					Task task = new Task(name + "-task"){
						@Override
						protected Object exec() throws Exception {
							for(Method method : methods){
								method.invoke(target);
							}
							return null;
						}
					};

					List<Task> list = new ArrayList<>();
					list.add(task);
					return list;
				}
			};
			
			//扫描FomContfig
			Field[] fields = clazz.getDeclaredFields();
			for(Field f : fields){
				FomConfig c = f.getAnnotation(FomConfig.class);
				if(c != null){
					String value = c.value();
					if(value.indexOf("${") != -1){ 
						value = SpringContext.getPropertiesValue(value);
					}
					
					f.setAccessible(true); 
					f.set(instance, value);
					
					String key = c.key();
					if(StringUtils.isBlank(key)){
						key = f.getName();
					}
					context.config.put(key, value); 
				}
			}
			
			//扫描PostConstruct
			for(Method method : clazz.getMethods()){
				PostConstruct con = method.getAnnotation(PostConstruct.class);
				if(con != null){
					method.invoke(instance);
				}
			}
			
			context.regist(LOADFROM_FOMSCHEDUL);
		}
	}

	@SuppressWarnings("serial")
	private static void loadFomSchedulBatch(String[] pckages) throws Exception {
		Set<Class<?>> clazzs = new HashSet<>();
		for(String pack : pckages){
			Reflections reflections = new Reflections(pack.trim());
			clazzs.addAll(reflections.getTypesAnnotatedWith(FomSchedulBatch.class));
		}
		
		SpringRegistry registry = SpringContext.getBean(SpringRegistry.class);
		for(Class<?> clazz : clazzs){
			if(!SchedulBatchFactory.class.isAssignableFrom(clazz)){
				LOG.warn("ignore context with @FomSchedulBatch, " + clazz + " isn't a implements of SchedulBatchFactory.");
				continue;
			}
			
			FomSchedulBatch fom = clazz.getAnnotation(FomSchedulBatch.class);
			
			String name = fom.name();
			if(StringUtils.isBlank(name)){
				name = clazz.getSimpleName();
			}
			
			final Object instance = clazz.newInstance();
			registry.regist(name + "-task", instance); 
			
			Map<String, String> map = new HashMap<>();
			map.put(ContextConfig.CONF_CRON, fom.cron());
			map.put(ContextConfig.CONF_THREADCORE, String.valueOf(fom.threadCore()));
			map.put(ContextConfig.CONF_THREADMAX, String.valueOf(fom.threadMax()));
			map.put(ContextConfig.CONF_QUEUESIZE, String.valueOf(fom.queueSize()));
			map.put(ContextConfig.CONF_OVERTIME, String.valueOf(fom.threadOverTime()));
			map.put(ContextConfig.CONF_ALIVETIME, String.valueOf(fom.threadAliveTime()));
			map.put(ContextConfig.CONF_CANCELLABLE, String.valueOf(fom.cancellable()));
			map.put(ContextConfig.CONF_EXECONLOAN, String.valueOf(fom.execOnLoad()));
			map.put(ContextConfig.CONF_STOPWITHNOCRON, String.valueOf(fom.stopWithNoCron()));
			map.put(ContextConfig.CONF_REMARK, String.valueOf(fom.remark()));

			createMap.putIfAbsent(name, map);
			
			Context context = new Context(name){

				private final SchedulBatchFactory factory = (SchedulBatchFactory)instance;

				@Override
				protected <E> Collection<? extends Task<E>> scheduleBatch() throws Exception {
					return factory.creatTasks();
				}
			};
			
			//扫描FomContfig
			Field[] fields = clazz.getDeclaredFields();
			for(Field f : fields){
				FomConfig c = f.getAnnotation(FomConfig.class);
				if(c != null){
					String value = c.value();
					if(value.indexOf("${") != -1){ 
						value = SpringContext.getPropertiesValue(value);
					}
					
					f.setAccessible(true); 
					f.set(instance, value);
					
					String key = c.key();
					if(StringUtils.isBlank(key)){
						key = f.getName();
					}
					context.config.put(key, value); 
				}
			}
			
			//扫描PostConstruct
			for(Method method : clazz.getMethods()){
				PostConstruct con = method.getAnnotation(PostConstruct.class);
				if(con != null){
					method.invoke(instance);
				}
			}
			
			context.regist(LOADFROM_FOMSCHEDULBATCH);
		}
	}
	

}
