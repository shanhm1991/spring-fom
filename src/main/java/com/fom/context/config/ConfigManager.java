package com.fom.context.config;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.fom.context.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class ConfigManager {

	private static final Logger LOG = LoggerFactory.getLogger("config");

	private static Map<String,Config> configMap = new ConcurrentHashMap<String,Config>();

	protected static Config get(String key) {
		return configMap.get(key);
	}

	public static void register(Config config){
		if(config == null){
			return;
		}
		if(null == configMap.put(config.getName(), config)){
			if(config.isValid()){
				LOG.info("\n");
				LOG.info("#加载配置: " + config.getName() + "\n" + config);
			}else{
				LOG.info("\n");
				LOG.warn("#非法配置: " + config.getName() + "\n" + config);
			}
		}else{
			LOG.info("\n");
			LOG.info("#更新配置: " + config.getName() + "\n" + config);
		}
	}

	protected static Collection<Config> getAll(){
		return configMap.values();
	}

	protected static Map<String,Config> getMap(){
		return configMap;
	}

}
