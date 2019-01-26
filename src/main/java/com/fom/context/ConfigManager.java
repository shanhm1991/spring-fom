package com.fom.context;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.fom.log.LoggerFactory;
import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 *
 */
public final class ConfigManager {

	private static final Logger LOG = LoggerFactory.getLogger("config");

	private static Map<String,Config> configMap = new ConcurrentHashMap<String,Config>();

	public static Config get(String key) {
		return configMap.get(key);
	}

	static void register(Config config){
		if(config == null){
			return;
		}
		if(null == configMap.put(config.name, config)){
			if(config.valid){
				LOG.info("\n");
				LOG.info("#加载配置: " + config.name + "\n" + config);
			}else{
				LOG.info("\n");
				LOG.warn("#非法配置: " + config.name + "\n" + config);
			}
		}else{
			LOG.info("\n");
			LOG.info("#更新配置: " + config.name + "\n" + config);
		}
	}

	protected static Collection<Config> getAll(){
		return configMap.values();
	}

	protected static Map<String,Config> getMap(){
		return configMap;
	}

	static Config load(Element element) {
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
			config.valid = config.isValid();
		}catch(Exception e){
			if(config != null){
				config.valid = false;
			}
			LOG.info("\n"); 
			LOG.error(name + "加载异常", e);
		}
		return config;
	}

	static void apply(Config config, Document doc) throws Exception { 
		File apply = new File(System.getProperty("config.apply"));
		for(File file : apply.listFiles()){
			if(file.getName().startsWith(config.name + ".xml.") && !file.delete()){
				throw new RuntimeException("删除文件失败:" + file.getName());
			}
		}
		File xml = new File(apply + File.separator + config.name + ".xml." + config.loadTime);
		XmlUtil.writeDocToFile(doc, xml);
		FileUtils.copyFile(xml, new File(apply + File.separator + "history" + File.separator + xml.getName()));
	}
}
