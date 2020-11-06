package org.eto.fom.context.core;

import java.io.File;
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
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.context.annotation.FomSchedulBatch;
import org.eto.fom.context.annotation.SchedulBatchFactory;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.gson.Gson;
import org.springframework.util.ReflectionUtils;

/**
 * context实例的管理
 *
 * @author shanhm
 *
 */
public class ContextManager {

	private static final Logger LOG = Logger.getLogger(ContextManager.class);

	static ConcurrentMap<String,Context<?>> loadedContext = new ConcurrentHashMap<>();

	public static boolean exist(String contextName){
		return loadedContext.containsKey(contextName);
	}

	public static void register(Context<?> context, boolean putConfig) throws Exception {
		if(context == null){
			return;
		}

		Context<?> exits = loadedContext.putIfAbsent(context.name, context);
		if(exits != null){
			LOG.warn("context[" + context.name + "] already exist, load ignored.");
			return;
		}

		SpringRegistry registry = SpringContext.getBean(SpringRegistry.class);
		registry.regist(context.name, context);

		Class<?> clazz = context.getClass();
		//扫描FomContfig
		for(Field f : clazz.getDeclaredFields()){
			Value v = f.getAnnotation(Value.class);
			if(v != null){
				valueField(f, context, v.value(), context, putConfig);
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
	 * @param xmlPath xmlPath
	 * @throws Exception Exception
	 */
	public static void load(String xmlPath) throws Exception{

		loadCache();

		List<String> pckages = FomConfiguration.packages;

		File xml = new File(xmlPath);
		if(xml.exists()){
			Map<String, Element> confMap = new HashMap<>();
			findConfig(xml, confMap, pckages);
			for(Entry<String,Element> entry : confMap.entrySet()){
				loadXml(entry.getKey(), entry.getValue());
			}
		}

		loadFomContexts(pckages);

		loadFomSchedul(pckages);

		loadFomSchedulBatch(pckages);

		for(Entry<String, Context<?>> entry : loadedContext.entrySet()){
			entry.getValue().startup();
		}
	}

	@SuppressWarnings("unchecked")
	private static void loadCache() throws Exception {
		String cache = System.getProperty("cache.context");
		File[] array = new File(cache).listFiles(File::isFile);
		if(null == array){
			return;
		}

		LOG.info("load context from cache：" + cache);
		Gson gson = new Gson();
		for(File file : array){
			try(FileInputStream input = new FileInputStream(file)){
				List<String> list = IOUtils.readLines(input);
				String json = list.get(0); //not null

				Map<String, Object> map = gson.fromJson(json, Map.class);
				String name = (String)map.get("name");
				String fom_context = (String)map.get("fom_context");
				String fom_schedul = (String)map.get("fom_schedul");
				String fom_schedulbatch = (String)map.get("fom_schedulbatch");

				Map<String, Map<String, String>> config = (Map<String, Map<String, String>>) map.get("config");
				Map<String, String> vMap= config.get("valueMap");
				ConcurrentHashMap<String, String> valueMap = new ConcurrentHashMap<>(vMap);
				if(StringUtils.isNotBlank(fom_context)){
					Context.localName.set(name);
					ContextConfig.loadedConfig.putIfAbsent(name, valueMap);
					Class<?> clazz = Class.forName(fom_context);
					Context<?> context = (Context<?>)clazz.newInstance();
					context.regist(false);
				}else if(StringUtils.isNotBlank(fom_schedul)){
					loadFomSchedul(Class.forName(fom_schedul), valueMap, false);
				}else if(StringUtils.isNotBlank(fom_schedulbatch)){
					loadFomSchedulBatch(Class.forName(fom_schedulbatch), valueMap, false);
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
					map.put(ContextConfig.CONF_FIXEDRATE, String.valueOf(fc.fixedRate()));
					map.put(ContextConfig.CONF_FIXEDDELAY, String.valueOf(fc.fixedDelay()));
					map.put(ContextConfig.CONF_THREADCORE, String.valueOf(fc.threadCore()));
					map.put(ContextConfig.CONF_THREADMAX, String.valueOf(fc.threadMax()));
					map.put(ContextConfig.CONF_QUEUESIZE, String.valueOf(fc.queueSize()));
					map.put(ContextConfig.CONF_OVERTIME, String.valueOf(fc.threadOverTime()));
					map.put(ContextConfig.CONF_ALIVETIME, String.valueOf(fc.threadAliveTime()));
					map.put(ContextConfig.CONF_CANCELLABLE, String.valueOf(fc.cancellable()));
					map.put(ContextConfig.CONF_EXECONLOAN, String.valueOf(fc.execOnLoad()));
					map.put(ContextConfig.CONF_STOPWITHNOSCHEDULE, String.valueOf(fc.stopWithNoSchedule()));
					map.put(ContextConfig.CONF_REMARK, fc.remark());
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
			Context<?> context = (Context<?>)clazz.newInstance();

			context.fom_context = classname;
			context.regist(true);
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
			map.put(ContextConfig.CONF_FIXEDRATE, String.valueOf(fc.fixedRate()));
			map.put(ContextConfig.CONF_FIXEDDELAY, String.valueOf(fc.fixedDelay()));
			map.put(ContextConfig.CONF_THREADCORE, String.valueOf(fc.threadCore()));
			map.put(ContextConfig.CONF_THREADMAX, String.valueOf(fc.threadMax()));
			map.put(ContextConfig.CONF_QUEUESIZE, String.valueOf(fc.queueSize()));
			map.put(ContextConfig.CONF_OVERTIME, String.valueOf(fc.threadOverTime()));
			map.put(ContextConfig.CONF_ALIVETIME, String.valueOf(fc.threadAliveTime()));
			map.put(ContextConfig.CONF_CANCELLABLE, String.valueOf(fc.cancellable()));
			map.put(ContextConfig.CONF_EXECONLOAN, String.valueOf(fc.execOnLoad()));
			map.put(ContextConfig.CONF_STOPWITHNOSCHEDULE, String.valueOf(fc.stopWithNoSchedule()));
			map.put(ContextConfig.CONF_REMARK, fc.remark());

			String name = fc.name();
			if(StringUtils.isBlank(name)){
				name = clazz.getSimpleName();
			}

			Context.localName.set(name);
			ContextConfig.loadedConfig.putIfAbsent(name, map);
			Context<?> context = (Context<?>)clazz.newInstance();

			context.fom_context = clazz.getName();
			context.regist(true);
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
			loadFomSchedul(clazz,  null, true);
		}
	}

	private static void loadFomSchedul(
			Class<?> clazz, ConcurrentHashMap<String, String> map, boolean putConfig) throws Exception {
		FomSchedul fc = clazz.getAnnotation(FomSchedul.class);
		String name = fc.name();
		if(StringUtils.isBlank(name)){
			name = clazz.getSimpleName();
		}

		if(loadedContext.containsKey(name)){
			LOG.warn("context[" + name + "] already exist, load ignored.");
			return;
		}

		String cron = fc.cron();
		long fixedRate = fc.fixedRate();
		long fixedDelay = fc.fixedDelay();

		List<Method> methods = new ArrayList<>();
		for(Method method : clazz.getMethods()){
			Scheduled sch = method.getAnnotation(Scheduled.class);
			if(sch != null){
				methods.add(method);
				if(StringUtils.isBlank(cron)){
					cron = sch.cron();
				}
				if(fixedRate <= 0){
					fixedRate = sch.fixedRate();
				}
				if(fixedDelay <= 0){
					fixedDelay = sch.fixedDelay();
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
			map.put(ContextConfig.CONF_FIXEDRATE, String.valueOf(fixedRate));
			map.put(ContextConfig.CONF_FIXEDDELAY, String.valueOf(fixedDelay));
			map.put(ContextConfig.CONF_THREADCORE, String.valueOf(fc.threadCore()));
			map.put(ContextConfig.CONF_THREADMAX, String.valueOf(fc.threadMax()));
			map.put(ContextConfig.CONF_QUEUESIZE, String.valueOf(fc.queueSize()));
			map.put(ContextConfig.CONF_OVERTIME, String.valueOf(fc.threadOverTime()));
			map.put(ContextConfig.CONF_ALIVETIME, String.valueOf(fc.threadAliveTime()));
			map.put(ContextConfig.CONF_CANCELLABLE, String.valueOf(fc.cancellable()));
			map.put(ContextConfig.CONF_EXECONLOAN, String.valueOf(fc.execOnLoad()));
			map.put(ContextConfig.CONF_STOPWITHNOSCHEDULE, String.valueOf(fc.stopWithNoSchedule()));
			map.put(ContextConfig.CONF_REMARK, fc.remark());
		}

		Context.localName.set(name);
		ContextConfig.loadedConfig.putIfAbsent(name, map);

		Context<?> context = new Context<Object>(){
			@Override
			protected Collection<Task<Object>> scheduleBatch() {
				Task<Object> task = new Task<Object>(name + "-task"){
					@Override
					public Object exec() throws Exception {
						for(Method method : methods){
							method.invoke(instance);
						}
						return null;
					}
				};

				List<Task<Object>> list = new ArrayList<>();
				list.add(task);
				return list;
			}
		};

		//扫描FomContfig
		for(Field f : clazz.getDeclaredFields()){
			Value v = f.getAnnotation(Value.class);
			if(v != null){
				valueField(f, instance, v.value(), context, putConfig);
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
		context.regist(putConfig);
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
			loadFomSchedulBatch(clazz, null, true);
		}
	}

	private static void loadFomSchedulBatch(
			Class<?> clazz, ConcurrentHashMap<String, String> map, boolean putConfig) throws Exception {
		if(!SchedulBatchFactory.class.isAssignableFrom(clazz)){
			LOG.warn("ignore context, " + clazz + " isn't a implements of SchedulBatchFactory.");
			return;
		}

		FomSchedulBatch fc = clazz.getAnnotation(FomSchedulBatch.class);
		String name = fc.name();
		if(StringUtils.isBlank(name)){
			name = clazz.getSimpleName();
		}

		if(loadedContext.containsKey(name)){
			LOG.warn("context[" + name + "] already exist, load ignored.");
			return;
		}

		SpringRegistry registry = SpringContext.getBean(SpringRegistry.class);
		final Object instance = clazz.newInstance();
		registry.regist(name + "-task", instance);

		if(map == null){
			map = new ConcurrentHashMap<>();
			map.put(ContextConfig.CONF_CRON, fc.cron());
			map.put(ContextConfig.CONF_FIXEDRATE, String.valueOf(fc.fixedRate()));
			map.put(ContextConfig.CONF_FIXEDDELAY, String.valueOf(fc.fixedDelay()));
			map.put(ContextConfig.CONF_THREADCORE, String.valueOf(fc.threadCore()));
			map.put(ContextConfig.CONF_THREADMAX, String.valueOf(fc.threadMax()));
			map.put(ContextConfig.CONF_QUEUESIZE, String.valueOf(fc.queueSize()));
			map.put(ContextConfig.CONF_OVERTIME, String.valueOf(fc.threadOverTime()));
			map.put(ContextConfig.CONF_ALIVETIME, String.valueOf(fc.threadAliveTime()));
			map.put(ContextConfig.CONF_CANCELLABLE, String.valueOf(fc.cancellable()));
			map.put(ContextConfig.CONF_EXECONLOAN, String.valueOf(fc.execOnLoad()));
			map.put(ContextConfig.CONF_STOPWITHNOSCHEDULE, String.valueOf(fc.stopWithNoSchedule()));
			map.put(ContextConfig.CONF_REMARK, fc.remark());
		}

		Context.localName.set(name);
		ContextConfig.loadedConfig.putIfAbsent(name, map);
		Context<Object> context = new Context<Object>(){
			@SuppressWarnings("unchecked")
			@Override
			protected Collection<? extends Task<Object>> scheduleBatch() throws Exception {
				return ((SchedulBatchFactory<Object>)instance).creatTasks();
			}
		};

		//扫描FomContfig
		Field[] fields = clazz.getDeclaredFields();
		for(Field f : fields){
			Value v = f.getAnnotation(Value.class);
			if(v != null){
				valueField(f, instance, v.value(), context, putConfig);
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
		context.regist(putConfig);
	}

	static void valueField(
			Field field, Object instance, String expression, Context<?> context, boolean putConfig) throws Exception{
		List<String> list = getProperties(expression);
		for(String ex : list){
			String springValue = SpringContext.getPropertiesValue(ex);

			int index = ex.indexOf(":");
			if(index == -1){
				index = ex.indexOf("}");
			}
			String key = ex.substring(2, index);
			String confValue = context.config.get(key);

			if(putConfig){
				context.config.put(key, springValue);
				expression = expression.replace(ex, springValue);
			}else{
				expression = expression.replace(ex, confValue);
			}
		}

		ReflectionUtils.makeAccessible(field);
		//ReflectionUtils.setField(field, instance, value);
		switch(field.getGenericType().toString()){
			case "short":
			case "class java.lang.Short":
				field.set(instance, Short.valueOf(expression));
				break;
			case "int":
			case "class java.lang.Integer":
				field.set(instance, Integer.valueOf(expression));
				break;
			case "long":
			case "class java.lang.Long":
				field.set(instance, Long.valueOf(expression));
				break;
			case "float":
			case "class java.lang.Float":
				field.set(instance, Float.valueOf(expression));
				break;
			case "double":
			case "class java.lang.Double":
				field.set(instance, Double.valueOf(expression));
				break;
			case "boolean":
			case "class java.lang.Boolean":
				field.set(instance, Boolean.valueOf(expression));
				break;
			case "class java.lang.String":
				field.set(instance, expression);
				break;
			default:
				throw new UnsupportedOperationException("不支持的配置类型：" + instance.getClass().getName() + "." + field.getName());
		}
	}

	private static List<String> getProperties(String a){
		List<String> list = new ArrayList<>();
		int begin;
		int end = 0;
		while((begin = a.indexOf("${", end)) != -1){
			end = a.indexOf("}", begin);
			list.add(a.substring(begin, end + 1));
		}
		return list;
	}

}
