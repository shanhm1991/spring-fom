package org.eto.fom.context.core;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
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



	static ConcurrentMap<String,Context> loadedContext = new ConcurrentHashMap<>();

	public static boolean exist(String contextName){
		return loadedContext.containsKey(contextName);
	}

	public static void register(Context context) throws Exception {
		if(context == null){
			return;
		}

		Context exits = loadedContext.putIfAbsent(context.name, context);
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

		for(Entry<String, Context> entry : loadedContext.entrySet()){
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

	@SuppressWarnings({ "unchecked" })
	private static void loadXmlContexts(String xmlPath, Element fom) throws Exception {
		Element contexts = fom.element("contexts");
		if(contexts == null){
			return;
		} 

		LOG.info("load context from: " + xmlPath); 
		Iterator<?> it = contexts.elementIterator("context");
		while(it.hasNext()){
			Element element = (Element)it.next();
			String name = element.attributeValue("name");
			String classname = element.attributeValue("class");

			if(StringUtils.isBlank(classname)){
				LOG.warn("ignore context[" + name + "], class can not be empty."); 
				continue;
			}

			ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
			Class<?> clazz = Class.forName(classname);
			if(StringUtils.isBlank(name)){ 
				FomContext fc = clazz.getAnnotation(FomContext.class);
				if(fc != null){
					name = fc.name();
					if(StringUtils.isBlank(name)){
						name = clazz.getSimpleName();
					}
					map.put(ContextConfig.CONF_CRON, fc.cron());
					map.put(ContextConfig.CONF_THREADCORE, String.valueOf(fc.threadCore()));
					map.put(ContextConfig.CONF_THREADMAX, String.valueOf(fc.threadMax()));
					map.put(ContextConfig.CONF_QUEUESIZE, String.valueOf(fc.queueSize()));
					map.put(ContextConfig.CONF_OVERTIME, String.valueOf(fc.threadOverTime()));
					map.put(ContextConfig.CONF_ALIVETIME, String.valueOf(fc.threadAliveTime()));
					map.put(ContextConfig.CONF_CANCELLABLE, String.valueOf(fc.cancellable()));
					map.put(ContextConfig.CONF_EXECONLOAN, String.valueOf(fc.execOnLoad()));
					map.put(ContextConfig.CONF_STOPWITHNOCRON, String.valueOf(fc.stopWithNoCron()));
					map.put(ContextConfig.CONF_REMARK, String.valueOf(fc.remark()));
				}else{
					name = clazz.getSimpleName();
				}
			}

			if(!Context.class.isAssignableFrom(clazz)){
				LOG.warn("ignore context[" + name + "], " + classname + " isn't a Context."); 
				continue;
			}

			// 以xml的配置为准，如果没有配置，则去注解的值，如果也没有注解，再使用默认值
			List<Element> elements = element.elements();
			for(Element e : elements){
				String value = e.getTextTrim();
				if(StringUtils.isBlank(value)){
					map.put(e.getName(), e.asXML());
				}else{
					map.put(e.getName(), e.getTextTrim());
				}
			}

			Context.localName.set(name); 
			ContextConfig.loadedConfig.putIfAbsent(name, map);
			Context context = (Context)clazz.newInstance();
			context.regist();
		}
	}

	private static void loadFomContexts(List<String> pckages) throws Exception{
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
			
			FomContext fc = clazz.getAnnotation(FomContext.class);
			ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
			map.put(ContextConfig.CONF_CRON, fc.cron());
			map.put(ContextConfig.CONF_THREADCORE, String.valueOf(fc.threadCore()));
			map.put(ContextConfig.CONF_THREADMAX, String.valueOf(fc.threadMax()));
			map.put(ContextConfig.CONF_QUEUESIZE, String.valueOf(fc.queueSize()));
			map.put(ContextConfig.CONF_OVERTIME, String.valueOf(fc.threadOverTime()));
			map.put(ContextConfig.CONF_ALIVETIME, String.valueOf(fc.threadAliveTime()));
			map.put(ContextConfig.CONF_CANCELLABLE, String.valueOf(fc.cancellable()));
			map.put(ContextConfig.CONF_EXECONLOAN, String.valueOf(fc.execOnLoad()));
			map.put(ContextConfig.CONF_STOPWITHNOCRON, String.valueOf(fc.stopWithNoCron()));
			map.put(ContextConfig.CONF_REMARK, String.valueOf(fc.remark()));
			
			String name = fc.name();
			if(StringUtils.isBlank(name)){
				name = clazz.getSimpleName();
			}

			Context.localName.set(name); 
			ContextConfig.loadedConfig.putIfAbsent(name, map);
			Context context = (Context)clazz.newInstance();
			context.regist();
		}
	}

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
			FomSchedul fc = clazz.getAnnotation(FomSchedul.class);
			String name = fc.name();
			if(StringUtils.isBlank(name)){
				name = clazz.getSimpleName();
			}

			String cron = fc.cron();
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

			//没有@Scheduled方法，或者没有设置cron的就忽略
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

			ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
			map.put(ContextConfig.CONF_CRON, cron);
			map.put(ContextConfig.CONF_THREADCORE, String.valueOf(fc.threadCore()));
			map.put(ContextConfig.CONF_THREADMAX, String.valueOf(fc.threadMax()));
			map.put(ContextConfig.CONF_QUEUESIZE, String.valueOf(fc.queueSize()));
			map.put(ContextConfig.CONF_OVERTIME, String.valueOf(fc.threadOverTime()));
			map.put(ContextConfig.CONF_ALIVETIME, String.valueOf(fc.threadAliveTime()));
			map.put(ContextConfig.CONF_CANCELLABLE, String.valueOf(fc.cancellable()));
			map.put(ContextConfig.CONF_EXECONLOAN, String.valueOf(fc.execOnLoad()));
			map.put(ContextConfig.CONF_STOPWITHNOCRON, String.valueOf(fc.stopWithNoCron()));
			map.put(ContextConfig.CONF_REMARK, String.valueOf(fc.remark()));
			
			Context.localName.set(name); 
			ContextConfig.loadedConfig.putIfAbsent(name, map);
			Context context = new Context(){
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				protected Collection<Task> scheduleBatch() throws Exception {
					Task task = new Task(name + "-task"){
						@Override
						protected Object exec() throws Exception {
							for(Method method : methods){
								method.invoke(instance);
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

			ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
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

			Context.localName.set(name); 
			ContextConfig.loadedConfig.putIfAbsent(name, map);
			Context context = new Context(){
				@Override
				protected <E> Collection<? extends Task<E>> scheduleBatch() throws Exception {
					return ((SchedulBatchFactory)instance).creatTasks();
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
