package org.eto.fom.context.core;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eto.fom.context.SpringContext;
import org.eto.fom.util.IoUtil;

/**
 * 
 * @author shanhm
 *
 */
class XmlUtil {

	/**
	 * 获取节点值
	 * @param el Element
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public static String getString(Element el, String key, String defaultValue){
		Element e = el.element(key);
		if(e == null){
			return defaultValue;
		}
		
		String value = e.getTextTrim();
		if(StringUtils.isBlank(value)){
			return defaultValue;
		}
		
		if(value.indexOf("${") != -1){ 
			value = SpringContext.getPropertiesValue(value);
		}
		return value;
	}

	/**
	 * 获取节点值
	 * @param el Element
	 * @param key key
	 * @param defaultValue defaultValue
	 * @param min min
	 * @param max max
	 * @return value
	 */
	public static int getInt(Element el, String key, int defaultValue, int min, int max){
		Element e = el.element(key);
		if(e == null){
			return defaultValue;
		}
		
		String v = e.getTextTrim();
		if(StringUtils.isBlank(v)){
			return defaultValue;
		}
		
		if(v.indexOf("${") != -1){ 
			v = SpringContext.getPropertiesValue(v);
		}
		
		int value = 0;
		try{
			value = Integer.parseInt(v);
		}catch(Exception e1){
			return defaultValue;
		}

		if(value < min || value > max){
			return defaultValue;
		}
		return value;
	}

	/**
	 * 获取节点值
	 * @param el Element
	 * @param key key
	 * @param defaultValue defaultValue
	 * @param min min
	 * @param max max
	 * @return value
	 */
	public static long getLong(Element el, String key, long defaultValue, long min, long max){
		Element e = el.element(key);
		if(e == null){
			return defaultValue;
		}
		
		String v = e.getTextTrim();
		if(StringUtils.isBlank(v)){
			return defaultValue;
		}
		
		if(v.indexOf("${") != -1){ 
			v = SpringContext.getPropertiesValue(v);
		}
		
		long value = 0;
		try{
			value = Long.parseLong(v);
		}catch(Exception e1){
			return defaultValue;
		}

		if(value < min || value > max){
			return defaultValue;
		}
		return value;
	}

	/**
	 * 获取节点值
	 * @param el Element
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return value
	 */
	public static boolean getBoolean(Element el, String key, boolean defaultValue){
		Element e = el.element(key);
		if(e == null){
			return defaultValue;
		}
		
		String v = e.getTextTrim();
		if(StringUtils.isBlank(v)){
			return defaultValue;
		}
		
		if(v.indexOf("${") != -1){ 
			v = SpringContext.getPropertiesValue(v);
		}

		boolean value = false;
		try{
			value = Boolean.parseBoolean(v);
		}catch(Exception e1){
			return defaultValue;
		}
		return value;
	}

	/**
	 * 将Document格式化写入file
	 * @param doc Document
	 * @param xml File
	 * @throws Exception Exception
	 */
	public static void writeDocToFile(Document doc, File xml) throws Exception { 
		OutputFormat formater=OutputFormat.createPrettyPrint();  
		formater.setEncoding("UTF-8");  
		FileOutputStream out = null;
		XMLWriter writer = null;
		try{
			out = new FileOutputStream(xml);
			writer=new XMLWriter(out,formater);
			writer.setEscapeText(false);
			writer.write(doc);  
			writer.flush();
			writer.close();
		}finally{
			IoUtil.close(out); 
		}
	}
}
