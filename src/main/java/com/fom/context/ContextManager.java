package com.fom.context;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.dom4j.Element;

/**
 * context实例的管理
 * 
 * @author shanhm
 *
 */
class ContextManager {

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
}
