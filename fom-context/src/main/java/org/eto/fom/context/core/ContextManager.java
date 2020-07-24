package org.eto.fom.context.core;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
import org.springframework.scheduling.annotation.Scheduled;

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

	public static ConcurrentMap<String,Context> contextMap = new ConcurrentHashMap<>();

	public static boolean exist(String contextName){
		return contextMap.containsKey(contextName);
	}

	public static void register(Context context) throws Exception {
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
		LOG.info("load context[" + context.name + "], " + context.config);
	}

	/**
	 * 加载缓存以及xmlPath配置文件中的context
	 * 
	 * @param xmlPath
	 * @throws Exception 
	 */
	public static void load(String xmlPath) throws Exception{ 

		loadCacheContexts(); 

		File xml = new File(xmlPath);
		if(!xml.exists()){
			LOG.info("cann't find fom config: " + xmlPath); 
			return;
		}

		Map<String, Element> confMap = new HashMap<>();
		List<String> pckages = new ArrayList<>();
		findConfig(xml, confMap, pckages);

		for(Entry<String,Element> entry : confMap.entrySet()){
			loadXmlContexts(entry.getKey(), entry.getValue());
		}

		loadFomContexts(pckages);

		loadFomSchedul(pckages);

		loadFomSchedulBatch(pckages);
		
		for(Entry<String, Context> entry : contextMap.entrySet()){
			entry.getValue().startup();
		}
	}

	private static void loadCacheContexts() {
		String cache = System.getProperty("cache.context");
		File[] array = new File(cache).listFiles(new FileFilter(){
			@Override
			public boolean accept(File file) {
				return file.isFile();
			}
		});
		if(ArrayUtils.isEmpty(array)){
			return;
		}

		LOG.info("load context from cache：" + cache); 
		for(File file : array){
			String name = file.getName().split("\\.")[0];
			ObjectInputStream input = null;
			try{
				input = new ObjectInputStream(new FileInputStream(file));
				Context context = (Context) input.readObject();
				context.unSerialize();
				context.regist();
			}catch(Exception e){
				LOG.error("load context[" + name + "] from cache failed", e);
			}finally{
				IoUtil.close(input);
			}
		}
	}

	private static void findConfig(File xml, Map<String, Element> confMap, List<String> packList) throws Exception{
		if(confMap.containsKey(xml.getPath())){
			return;
		}

		SAXReader reader = new SAXReader();
		reader.setEncoding("UTF-8");
		Document doc = reader.read(new FileInputStream(xml));
		Element root = doc.getRootElement();
		confMap.put(xml.getPath(), root);

		Element scan = root.element("fom-scan");
		if(scan != null){
			String fomscan = scan.getTextTrim();
			if(StringUtils.isNotBlank(fomscan)){
				String[] pckages = fomscan.split(",");
				for(String pack : pckages){
					mergePack(pack.trim(), packList);
				}
			}
		}

		Element includes = root.element("includes");
		if(includes != null){
			Iterator<?> it = includes.elementIterator("include");
			while(it.hasNext()){
				Element include = (Element)it.next();
				String location = include.getTextTrim();
				Resource[] resources = SpringContext.getResources(location);
				if(ArrayUtils.isNotEmpty(resources)){
					for(Resource resource : resources){
						findConfig(resource.getFile(), confMap, packList);
					}
				}
			}
		}
	}

	private static void mergePack(String pack, List<String> packList){
		if(packList.isEmpty()){
			packList.add(pack);
		}

		ListIterator<String> it = packList.listIterator();
		while(it.hasNext()){
			String exist = it.next();
			if(pack.startsWith(exist)){
				return;
			}else if(exist.startsWith(pack)){
				it.remove();
				it.add(pack);
				return;
			}
		}
		packList.add(pack);
	}

	@SuppressWarnings("rawtypes")
	private static void loadXmlContexts(String xmlPath, Element fom) {
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
				LOG.warn("ignore context[" + name + "], class can not be empty."); 
				continue;
			}

			try {
				Class<?> contextClass = Class.forName(clazz);
				if(!Context.class.isAssignableFrom(contextClass)){
					LOG.warn("ignore context[" + name + "], " + clazz + " isn't a Context."); 
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
					context.regist();
				}else{
					Constructor constructor = contextClass.getConstructor(String.class);
					Context context = (Context)constructor.newInstance(name);
					context.regist();
				}
			} catch (Exception e) {
				LOG.error("load context[" + name + "] failed", e);
			}
		}
	}

	//	private static void loadxmlIncludes(Element fom, String xmlPath) throws IOException {
	//		Element includes = fom.element("includes");
	//		if(includes == null){
	//			return;
	//		}
	//
	//		Iterator<?> it = includes.elementIterator("include");
	//		while(it.hasNext()){
	//			Element element = (Element)it.next();
	//
	//
	//			Resource r = SpringContext.getResource("/");
	//			System.out.println(r.getFile().getPath());
	//
	//			Resource[] arr = SpringContext.getResources("**/fom*.xml");
	//			System.out.println(Arrays.asList(arr)); 
	//
	//			File xml = new File(location);
	//			if(!xml.exists() || !xml.isFile()){
	//				LOG.warn("cann't find config: " + location);  
	//				continue;
	//			}
	//
	//			try{
	//				SAXReader reader = new SAXReader();
	//				reader.setEncoding("UTF-8");
	//				Document doc = reader.read(new FileInputStream(xml));
	//				Element root = doc.getRootElement();
	//				loadXmlContexts(root, location);
	//			}catch(Exception e){
	//				LOG.error("", e); 
	//			}
	//		}
	//	}

	private static void loadFomContexts(List<String> pckages){
		Set<Class<?>> clazzs = new HashSet<>();
		for(String pack : pckages){
			Reflections reflections = new Reflections(pack.trim());
			clazzs.addAll(reflections.getTypesAnnotatedWith(FomContext.class));
		}
		
		if(clazzs.isEmpty()){
			return;
		}

		LOG.info("load context with @FomContext"); 
		for(Class<?> clazz : clazzs){
			if(!Context.class.isAssignableFrom(clazz)){
				LOG.warn("ignore context, " + clazz + " isn't a Context");
				continue;
			}

			try {
				Context context = (Context)clazz.newInstance();
				context.regist();
			} catch (Exception e) {
				LOG.error("load context failed", e);
			} 
		}
	}

	@SuppressWarnings({ "serial" })
	private static void loadFomSchedul(List<String> pckages) throws Exception {
		Set<Class<?>> clazzs = new HashSet<>();
		for(String pack : pckages){
			Reflections reflections = new Reflections(pack.trim());
			clazzs.addAll(reflections.getTypesAnnotatedWith(FomSchedul.class));
		}
		
		if(clazzs.isEmpty()){
			return;
		}

		LOG.info("load context with @FomSchedul"); 
		SpringRegistry registry = SpringContext.getBean(SpringRegistry.class);
		for(Class<?> clazz : clazzs){
			FomSchedul fom = clazz.getAnnotation(FomSchedul.class);

			String name = fom.name();
			if(StringUtils.isBlank(name)){
				name = clazz.getSimpleName();
			}

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
				LOG.warn("ignore context, " + clazz + " hasn't Scheduled method");
				continue;
			}

			if(StringUtils.isBlank(cron)){
				LOG.warn("ignore context, " + clazz + " hasn't cron expression.");
				continue;
			}
			
			final Object instance = clazz.newInstance();
			registry.regist(name + "-task", instance); 

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

			context.regist();
		}
	}

	@SuppressWarnings("serial")
	private static void loadFomSchedulBatch(List<String> pckages) throws Exception {
		Set<Class<?>> clazzs = new HashSet<>();
		for(String pack : pckages){
			Reflections reflections = new Reflections(pack.trim());
			clazzs.addAll(reflections.getTypesAnnotatedWith(FomSchedulBatch.class));
		}
		
		if(clazzs.isEmpty()){
			return;
		}

		LOG.info("load context with @FomSchedulBatch"); 
		SpringRegistry registry = SpringContext.getBean(SpringRegistry.class);
		for(Class<?> clazz : clazzs){
			if(!SchedulBatchFactory.class.isAssignableFrom(clazz)){
				LOG.warn("ignore context, " + clazz + " isn't a implements of SchedulBatchFactory.");
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
			context.regist();
		}
	}

}
