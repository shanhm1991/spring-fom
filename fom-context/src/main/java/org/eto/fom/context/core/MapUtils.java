package org.eto.fom.context.core;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eto.fom.context.SpringContext;

/**
 * 
 * @author shanhm
 */
class MapUtils {

	/**
	 * 拷贝map中的值
	 * @param key key
	 * @param srcMap srcMap
	 * @param destMap destMap
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> void copyIfNotNull(T key, Map srcMap, Map destMap){
		Object value = srcMap.get(key);
		if(value != null){
			destMap.put(key, value);
		}
	}

	/**
	 * 拷贝map中的非空字符串值
	 * @param key key
	 * @param srcMap srcMap
	 * @param destMap destMap
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> void copyStringIfNotNull(T key, Map srcMap, Map destMap){
		String value = (String)srcMap.get(key);
		if(!StringUtils.isBlank(value)){
			destMap.put(key, value);
		}
	}


	/**
	 * 获取int值
	 * @param key key
	 * @param map map
	 * @param defaultValue defaultValue
	 * @return int
	 */
	@SuppressWarnings("rawtypes")
	public static <T> int getInt(T key, Map map, int defaultValue){
		if(!map.containsKey(key)){
			return defaultValue;
		}
		
		String v = String.valueOf(map.get(key));
		if(v.contains("${")){
			v = SpringContext.getPropertiesValue(v);
		}
		
		try{
			return Integer.parseInt(v);
		}catch(NumberFormatException e){
			return defaultValue;
		}
	}

	/**
	 * 获取long值
	 * @param key key
	 * @param map map
	 * @param defaultValue defaultValue
	 * @return long
	 */
	@SuppressWarnings("rawtypes")
	public static <T> long getLong(T key, Map map, long defaultValue){
		if(!map.containsKey(key)){
			return defaultValue;
		}
		
		String v = String.valueOf(map.get(key));
		if(v.contains("${")){
			v = SpringContext.getPropertiesValue(v);
		}
		
		try{
			return Long.parseLong(v);
		}catch(NumberFormatException e){
			return defaultValue;
		}
	}

	/**
	 * 获取boolean值
	 * @param key key
	 * @param map map
	 * @param defaultValue defaultValue
	 * @return boolean
	 */
	@SuppressWarnings("rawtypes")
	public static <T> boolean getBoolean(T key, Map map, boolean defaultValue){
		if(!map.containsKey(key)){
			return defaultValue;
		}
		
		String v = String.valueOf(map.get(key));
		if(v.contains("${")){
			v = SpringContext.getPropertiesValue(v);
		}
		
		try{
			return Boolean.parseBoolean(v);
		}catch(NumberFormatException e){
			return defaultValue;
		}
	}
	
	/**
	 * 获取string值
	 * @param key key
	 * @param map map
	 * @param defaultValue defaultValue
	 * @return String
	 */
	@SuppressWarnings("rawtypes")
	public static <T> String getString(T key, Map map, String defaultValue){
		if(!map.containsKey(key)){
			return defaultValue;
		}
		
		String v = String.valueOf(map.get(key));
		if(v.contains("${")){
			v = SpringContext.getPropertiesValue(v);
		}
		return v;
	}
}
