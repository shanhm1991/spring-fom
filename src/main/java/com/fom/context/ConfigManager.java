package com.fom.context;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 */
final class ConfigManager {

	private static final Logger LOG = LoggerFactory.getLogger("config");

	private static Map<String,Config> configMap = new ConcurrentHashMap<String,Config>();

	public static Config get(String key) {
		return configMap.get(key);
	}

	public static Collection<Config> getAll(){
		return configMap.values();
	}

	public static Map<String,Config> getMap(){
		return configMap;
	}

	public static void register(Config config){
		if(config == null){
			return;
		}
		if(configMap.put(config.name, config) == null){
			LOG.info("\n");
			LOG.info("#加载配置: " + config.name + "\n" + config);
		}else {
			LOG.info("\n");
			LOG.info("#更新配置: " + config.name + "\n" + config); 
		}
	}

	public static Config load(Element element) {
		String name = element.attributeValue("name");
		String clzz = element.attributeValue("config");
		Config config = null;
		try{
			Class<?> configClass = Class.forName(clzz);
			Constructor<?> constructor = configClass.getDeclaredConstructor(String.class); 
			constructor.setAccessible(true); 
			config = (Config)constructor.newInstance(name);
			config.loadTime = System.currentTimeMillis();
			config.element = element;
			config.load();
			if(!config.isValid()){
				LOG.info("\n"); 
				LOG.error(name + "校验失败");
			}
		}catch(Exception e){
			LOG.info("\n"); 
			LOG.error(name + "加载异常", e);
		}
		return config;
	}

}
