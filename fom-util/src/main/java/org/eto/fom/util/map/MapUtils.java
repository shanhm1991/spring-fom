package org.eto.fom.util.map;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author shanhm
 */
public class MapUtils {

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
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> int getInt(T key, Map map, int defaultValue){
		try{
			return Integer.parseInt(String.valueOf(map.get(key)));
		}catch(NumberFormatException e){
			return defaultValue;
		}
	}

	/**
	 * 获取long值
	 * @param key key
	 * @param map map
	 * @param defaultValue defaultValue
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> long getLong(T key, Map map, long defaultValue){
		try{
			return Long.parseLong(String.valueOf(map.get(key)));
		}catch(NumberFormatException e){
			return defaultValue;
		}
	}

	/**
	 * 获取boolean值
	 * @param key key
	 * @param map map
	 * @param defaultValue defaultValue
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> boolean getBoolean(T key, Map map, boolean defaultValue){
		try{
			return Boolean.parseBoolean(String.valueOf(map.get(key)));
		}catch(NumberFormatException e){
			return defaultValue;
		}
	}
	
	/**
	 * 获取string值
	 * @param key key
	 * @param map map
	 * @param defaultValue defaultValue
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> String getString(T key, Map map, String defaultValue){
		Object obj = map.get(key);
		if(obj == null){
			return defaultValue;
		}
		return String.valueOf(obj);
	}
}
