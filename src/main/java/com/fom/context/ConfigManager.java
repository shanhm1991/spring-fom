package com.fom.context;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.fom.util.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
class ConfigManager {
	
	private static final Logger LOG = LoggerFactory.getLogger("config");

	private static Map<String,Config> configMap = new ConcurrentHashMap<String,Config>();

	private ConfigManager(){

	}

	public static Config getConfig(String key) {
		return configMap.get(key);
	}

	public static void registerConfig(Config config){
		if(config == null){
			return;
		}
		if(null == configMap.put(config.name, config)){
			LOG.info("#加载配置: " + config.name + "\n" + config);
		}else{
			LOG.info("#更新配置: " + config.name + "\n" + config);
		}
	}

	public static Collection<Config> getAllConfig(){
		return configMap.values();
	}
	
	public static Map<String,Config> getConfigMap(){
		return configMap;
	}

}
