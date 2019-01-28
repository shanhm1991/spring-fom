package com.fom.context;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 */
final class ConfigManager {

	private static final Logger LOG = LoggerFactory.getLogger("context");

	private static Map<String,RuntimeConfig> configMap = new ConcurrentHashMap<String,RuntimeConfig>();

	public static RuntimeConfig get(String key) {
		return configMap.get(key);
	}

	public static Collection<RuntimeConfig> getAll(){
		return configMap.values();
	}

	public static Map<String,RuntimeConfig> getMap(){
		return configMap;
	}

	public static boolean register(RuntimeConfig config){
		if(config == null){
			return false;
		}
		if(!config.valid){
			LOG.warn("config[" + config.name + "]非法,取消加载.");
			return false;
		}
		if(configMap.containsKey(config.name)){
			LOG.warn("config[" + config.name + "]已经存在,取消加载."); 
			return false;
		}
		configMap.put(config.name, config);
		LOG.info("加载config[" + config.name + "]" + config);
		return true;
	}

	public static boolean update(RuntimeConfig config){
		if(config == null){
			return false;
		}
		if(!config.valid){
			LOG.warn("config[" + config.name + "]非法,取消更新.");
			return false;
		}
		if(!configMap.containsKey(config.name)){
			LOG.warn("config[" + config.name + "]不存在,取消更新.");
			return false;
		}
		configMap.put(config.name, config);
		LOG.info("更新config[" + config.name + "]\n" + config);
		return true;
	}

	public static RuntimeConfig load(Element element) { 
		RuntimeConfig config = null;
		String name = "";
		try{
			name = element.attributeValue("name");
			String clzz = element.attributeValue("class");
			if(StringUtils.isBlank(name) || StringUtils.isBlank(clzz)){
				LOG.warn("非法config配置:" + name + "=" + clzz); 
				return null;
			}
			Class<?> configClass = Class.forName(clzz);
			Constructor<?> constructor = configClass.getDeclaredConstructor(String.class); 
			constructor.setAccessible(true); 
			config = (RuntimeConfig)constructor.newInstance(name);
			config.loadTime = System.currentTimeMillis();
			config.element = element;
			config.load();
		}catch(Exception e){
			LOG.warn("[" + name + "]config初始化异常", e);
		}
		return config;
	}

}
