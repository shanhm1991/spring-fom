package com.fom.context;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * 
 * @author shanhm
 *
 */
public class ContextManager {

	private static final Logger LOG = Logger.getRootLogger();

	private static Map<String,Context> contextMap = new ConcurrentHashMap<>();

	static boolean exist(String contextName){
		return contextMap.containsKey(contextName);
	}
	
	static void register(Context context){
		if(context == null){
			return;
		}
		contextMap.put(context.name, context);
		LOG.info("加载context[" + context.name + "]");
	}
	
	static void startAll(){
		for(Entry<String, Context> entry : contextMap.entrySet()){
			LOG.info("启动context[" + entry.getKey() + "]");
			entry.getValue().start();
		}
		
	}
}
