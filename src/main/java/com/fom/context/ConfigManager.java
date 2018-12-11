package com.fom.context;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author shanhm1991
 *
 */
public class ConfigManager {
	
	private static Map<String,Config> configMap = new ConcurrentHashMap<String,Config>();
	
	private ConfigManager(){
		
	}
	
	public static Config getConfig(String key) {
		return configMap.get(key);
	}
	
	public static void registerConfig(Config config){
		configMap.put(config.name, config);
	}
	
	public static Collection<Config> getAllConfig(){
		return configMap.values();
	}

}
