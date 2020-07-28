package org.eto.fom.context.core;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
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

import org.apache.commons.io.IOUtils;
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
import org.reflections.Reflections;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.gson.Gson;

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

	public static void register(Context context, boolean configValued) throws Exception {
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
		if(!configValued){
			//扫描FomContfig
			Field[] fields = clazz.getDeclaredFields();
			for(Field f : fields){
				FomConfig c = f.getAnnotation(FomConfig.class);
				if(c != null){
					valueField(f, context, c.key(), c.value(), context);
				}
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

		loadCache(); 

		File xml = new File(xmlPath);
		if(!xml.exists()){
			LOG.info("cann't find fom config: " + xmlPath); 
			return;
		}

		Map<String, Element> confMap = new HashMap<>();
		List<String> pckages = new ArrayList<>();
		findConfig(xml, confMap, pckages);

		for(Entry<String,Element> entry : confMap.entrySet()){
			loadXml(entry.getKey(), entry.getValue());
		}

		loadFomContexts(pckages);

		loadFomSchedul(pckages);

		loadFomSchedulBatch(pckages);

		for(Entry<String, Context> entry : loadedContext.entrySet()){
			entry.getValue().startup();
		}
	}

	@SuppressWarnings("unchecked")
	private static void loadCache() throws Exception {
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
		Gson gson = new Gson();
		for(File file : array){
			try(FileInputStream input = new FileInputStream(file)){
				List<String> list = IOUtils.readLines(input);
				String json = list.get(0); //not null
				
				Map<String, String> map = gson.fromJson(json, Map.class);
				String fom_context = map.get("fom_context");
				String fom_schedul = map.get("fom_schedul");
				String fom_schedulbatch = map.get("fom_schedulbatch");
				
				if(StringUtils.isNotBlank(fom_context)){
					Class<?> clazz = Class.forName(fom_context);
					Context context = (Context)gson.fromJson(json, clazz);
					context.init();
					context.regist(true);
				}else if(StringUtils.isNotBlank(fom_schedul)){
					Context context = (Context)gson.fromJson(json, Context.class); //就为了获取configmap，这里实现不太优雅，暂且忍了
					loadFomSchedul(Class.forName(fom_schedul), context.config.valueMap);
				}else if(StringUtils.isNotBlank(fom_schedulbatch)){
					Context context = (Context)gson.fromJson(json, Context.class); 
					loadFomSchedulBatch(Class.forName(fom_schedulbatch), context.config.valueMap);
				}else{
					LOG.warn("not valid cache file：" + file.getName()); 
				}
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
	private static void loadXml(String xmlPath, Element fom) throws Exception {
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

			ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
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
			
			context.fom_context = classname;
			context.regist(false);
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
			ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
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
			
			context.fom_context = clazz.getName();
			context.regist(false);
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
		for(Class<?> clazz : clazzs){
			loadFomSchedul(clazz,  null);
		}
	}
	
	private static void loadFomSchedul(Class<?> clazz, ConcurrentHashMap<String, String> map) throws Exception {
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
			return;
		}

		if(StringUtils.isBlank(cron)){
			LOG.warn("ignore context, " + clazz + " hasn't cron expression.");
			return;
		}

		SpringRegistry registry = SpringContext.getBean(SpringRegistry.class);
		final Object instance = clazz.newInstance();
		registry.regist(name + "-task", instance); 

		if(map == null){
			map = new ConcurrentHashMap<>();
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
		}
		
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
				valueField(f, instance, c.key(), c.value(), context);
			}
		}

		//扫描PostConstruct
		for(Method method : clazz.getMethods()){
			PostConstruct con = method.getAnnotation(PostConstruct.class);
			if(con != null){
				method.invoke(instance);
			}
		}

		context.fom_schedul = clazz.getName();
		context.regist(false);
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
		for(Class<?> clazz : clazzs){
			loadFomSchedulBatch(clazz, null);
		}
	}
	
	private static void loadFomSchedulBatch(Class<?> clazz, ConcurrentHashMap<String, String> map) throws Exception {
		if(!SchedulBatchFactory.class.isAssignableFrom(clazz)){
			LOG.warn("ignore context, " + clazz + " isn't a implements of SchedulBatchFactory.");
			return;
		}

		FomSchedulBatch fom = clazz.getAnnotation(FomSchedulBatch.class);
		String name = fom.name();
		if(StringUtils.isBlank(name)){
			name = clazz.getSimpleName();
		}

		SpringRegistry registry = SpringContext.getBean(SpringRegistry.class);
		final Object instance = clazz.newInstance();
		registry.regist(name + "-task", instance); 

		if(map == null){
			map = new ConcurrentHashMap<>();
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
		}

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
				valueField(f, instance, c.key(), c.value(), context);
			}
		}

		//扫描PostConstruct
		for(Method method : clazz.getMethods()){
			PostConstruct con = method.getAnnotation(PostConstruct.class);
			if(con != null){
				method.invoke(instance);
			}
		}
		
		context.fom_schedulbatch = clazz.getName();
		context.regist(false);
	}
	
	private static void valueField(Field field, Object instance, String key, String value, Context context) throws Exception{
		if(value.indexOf("${") != -1){ 
			value = SpringContext.getPropertiesValue(value);
		}
		
		field.setAccessible(true);
		switch(field.getGenericType().toString()){
		case "short":
		case "class java.lang.Short":
			field.set(instance, Short.valueOf(value));
			break;
		case "int":
		case "class java.lang.Integer":
			field.set(instance, Integer.valueOf(value));
			break;
		case "long":
		case "class java.lang.Long":
			field.set(instance, Long.valueOf(value));
			break;
		case "float":
		case "class java.lang.Float":
			field.set(instance, Float.valueOf(value));
			break;
		case "double":
		case "class java.lang.Double":
			field.set(instance, Double.valueOf(value));
			break;
		case "boolean":
		case "class java.lang.Boolean":
			field.set(instance, Boolean.valueOf(value));
			break;
		case "class java.lang.String":
			field.set(instance, value);
			break;
	    default:
			throw new UnsupportedOperationException("不支持的配置类型：" + instance.getClass().getName() + "." + field.getName());
		}
		
		if(StringUtils.isBlank(key)){
			key = field.getName();
		}
		context.config.put(key, value); 
	}

}
