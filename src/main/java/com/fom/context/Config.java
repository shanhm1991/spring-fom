package com.fom.context;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.quartz.CronExpression;

import com.fom.log.LoggerFactory;
import com.fom.util.XmlUtil;

/**
 * remark 备注
 * cron   执行时机的定时表达式<br>
 * pattern 匹配资源名称的正则表达式<br>
 * context      处理资源的实现<br>
 * thread.min   处理线程最小数<br>
 * thread.max   处理线程最大数<br>
 * thread.aliveTime  处理线程空闲存活最长时间<br>
 * thread.overTime   处理线程执行超时时间<br>
 * thread.cancellable    处理线程执行超时是否中断<br>
 * 
 * @author shanhm
 *
 */
public abstract class Config {

	protected static final Logger LOG = LoggerFactory.getLogger("config");

	protected final String name;

	Element element;

	Element extendsElement;

	String remark;

	String regex;
	
	String cron;

	String context;

	int core;

	int max;

	int aliveTime;

	int overTime;

	boolean cancellable;

	protected Config(String name){
		this.name = name;
	}

	void load() throws Exception {
		remark = load(element, "remark", "");
		regex = load(element, "pattern", "");
		cron = load(element, "cron", "");
		context = load(element, "context", "");
		core = load(element, "thread.core", 4, 1, 10); 
		max = load(element, "thread.max", 20, 10, 50);
		aliveTime = load(element, "thread.aliveTime", 30, 3, 300);
		overTime = load(element, "thread.overTime", 3600, 300, 86400);
		cancellable = load(element, "thread.cancellable", false); 

		extendsElement = element.element("extends");
		loadExtends();
	}
	
	private Pattern pattern;

	private CronExpression cronExpression;
	
	boolean valid;
	
	long loadTime;

	boolean isValid() throws Exception {
		if(!StringUtils.isBlank(regex)){
			pattern = Pattern.compile(regex);
		}
		if(!StringUtils.isBlank(cron)){
			cronExpression = new CronExpression(cron);
		}
		return valid();
	}

		public final CronExpression getCron(){
		return cronExpression;
	}

	public final Pattern getPattern(){
		return pattern;
	}

	public final String getXml(){
		return element.asXML();
	}

	private String load(Element e, String key, String defaultValue) {
		String value =  XmlUtil.getString(e, key, defaultValue);
		entryMap.put(key, value);
		return value;
	}

	private int load(Element e, String key, int defaultValue, int min, int max){
		int value = XmlUtil.getInt(e, key, defaultValue, min, max);
		entryMap.put(key, String.valueOf(value)); 
		return value;
	}

	private long load(Element e, String key, long defaultValue, long min, long max){
		long value = XmlUtil.getLong(e, key, defaultValue, min, max);
		entryMap.put(key, String.valueOf(value)); 
		return value;
	}

	private boolean load(Element e, String key, boolean defaultValue){
		boolean value = XmlUtil.getBoolean(e, key, defaultValue);
		entryMap.put(key, String.valueOf(value)); 
		return value;
	}

	/**
	 * 加载<extends>中的String配置
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	protected final String loadExtends(String key, String defaultValue) {
		return load(extendsElement, key, defaultValue);
	}

	/**
	 * 加载<extends>中的int配置
	 * @param key
	 * @param defaultValue
	 * @param min
	 * @param max
	 * @return
	 */
	protected final int loadExtends(String key, int defaultValue, int min, int max){
		return load(extendsElement, key, defaultValue, min, max);
	}

	/**
	 * 加载<extends>中的long配置
	 * @param key
	 * @param defaultValue
	 * @param min
	 * @param max
	 * @return
	 */
	protected final long loadExtends(String key, long defaultValue, long min, long max){
		return load(extendsElement, key, defaultValue, min, max);
	}

	/**
	 * 加载<extends>中的boolean配置
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	protected final boolean loadExtends(String key, boolean defaultValue){
		return load(extendsElement, key, defaultValue);
	}

	/**
	 * 子类自定义加载<extends>中的配置
	 * @throws Exception
	 */
	protected abstract void loadExtends() throws Exception;

	/**
	 * 子类自定义校验<extends>中的配置加载结果，默认返回true
	 * @return
	 * @throws Exception
	 */
	protected boolean valid() throws Exception {
		return true;
	}

	private Map<String,String> entryMap = new LinkedHashMap<>();

	@Override
	public final boolean equals(Object object){
		if(!(object instanceof Config)){
			return false;
		}
		if(object == this){
			return true;
		}
		Config config = (Config)object;
		return entryMap.equals(config.entryMap);
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<String, String>> it = entryMap.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, String> entry = it.next();
			builder.append("\n" + entry.getKey() + "=" + entry.getValue());
		}
		return builder.toString();
	}

}
