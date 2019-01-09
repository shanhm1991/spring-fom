package com.fom.context;

import java.util.Map;

import com.fom.context.config.Config;
import com.fom.context.config.ConfigManager;

class InnerConfigManager extends ConfigManager {

	public static Config getConfig(String key) {
		return get(key);
	}
	
	public static Map<String,Config> getConfigMap() {
		return getMap();
	}
}
