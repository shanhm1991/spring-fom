package com.fom.context;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
class ConfigManager {

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
	
	public static Map<String,Config> getConfigMap(){
		return configMap;
	}

}
