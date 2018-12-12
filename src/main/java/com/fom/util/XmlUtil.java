package com.fom.util;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
public class XmlUtil {

	public static final String getString(Element el, String key, String defaultValue){
		Element e = el.element(key);
		if(e == null){
			return defaultValue;
		}
		String value = e.getTextTrim();
		if(StringUtils.isBlank(value)){
			return defaultValue;
		}
		return value;
	}
	
	public static final int getInt(Element el, String key, int defaultValue, int min, int max){
		Element e = el.element(key);
		if(e == null){
			return defaultValue;
		}
		int value = 0;
		try{
			value = Integer.parseInt(e.getTextTrim());
		}catch(Exception e1){
			return defaultValue;
		}

		if(value < min || value > max){
			return defaultValue;
		}
		return value;
	}
	
	public static final boolean getBoolean(Element el, String key, boolean defaultValue){
		Element e = el.element(key);
		if(e == null){
			return defaultValue;
		}

		boolean value = false;
		try{
			value = Boolean.parseBoolean(e.getTextTrim());
		}catch(Exception e1){
			return defaultValue;
		}
		return value;
	}
}
