package com.fom.context;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.dom4j.Element;

/**
 * 
 * @author shanhm
 *
 */
public class ContextManager {

	private static final Logger LOG = Logger.getRootLogger();

	//容器启动时会给elementMap赋值，Context构造时尝试从中获取配置
	static final Map<String, Element> elementMap = new ConcurrentHashMap<>();

	static Map<String,Context> contextMap = new ConcurrentHashMap<>();

	static boolean exist(String contextName){
		return contextMap.containsKey(contextName);
	}

	static void register(Context context){
		if(context == null){
			return;
		}
		contextMap.put(context.name, context);
		LOG.info("init context[" + context.name + "]");
	}

	static void startAll(){
		for(Entry<String, Context> entry : contextMap.entrySet()){
			LOG.info("start context[" + entry.getKey() + "]");
			entry.getValue().start();
		}

	}
}
