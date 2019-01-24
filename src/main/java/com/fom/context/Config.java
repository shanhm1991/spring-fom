package com.fom.context;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.quartz.CronExpression;

import com.fom.log.LoggerFactory;
import com.fom.util.XmlUtil;

/**
 * src.path    源文件目录<br>
 * src.pattern 源文件正则表达式<br>
 * src.match.fail.del  源文件匹配失败是否删除<br>
 * scanner.cron  扫描源目录的cron表达式<br>
 * scanner       扫描源目录的实现方式<br>
 * executor      处理文件的实现方式<br>
 * executor.min  任务线程最小数<br>
 * executor.max  任务线程最大数<br>
 * executor.aliveTime.seconds  任务线程空闲存活最长时间<br>
 * executor.overTime.seconds   任务线程执行超时时间<br>
 * executor.overTime.cancle    任务线程执行超时是否中断<br>
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public abstract class Config implements IConfig {

	protected static final Logger LOG = LoggerFactory.getLogger("config");

	/**
	 * 模块名称
	 */
	protected final String name;

	String srcUri;

	private Pattern pattern;

	private CronExpression cron;

	private boolean delMatchFail;

	String scannerClass;

	String contextClass;

	int core;

	int max;

	int aliveTime;

	int overTime;

	boolean cancellable;

	Element element;

	Element extendsElement;

	boolean valid;

	long loadTime;

	long startTime;

	volatile boolean isRunning = false;

	@SuppressWarnings("rawtypes")
	Scanner scanner;

	protected Config(String name){
		this.name = name;
	}

	void load() throws Exception {
		srcUri = ContextUtil.getEnvStr(load(element, "src.uri", ""));
		String regex = load(element, "src.pattern", "");
		if(!StringUtils.isBlank(regex)){
			pattern = Pattern.compile(regex);
		}
		delMatchFail = load(element, "src.delMatchFail", false);

		scannerClass = load(element, "scanner", ""); 
		String strCron = load(element, "scanner.cron", "");
		cron = new CronExpression(strCron);

		contextClass = load(element, "context", "");
		core = load(element, "thread.core", 4, 1, 10); 
		max = load(element, "thread.max", 20, 10, 50);
		aliveTime = load(element, "thread.aliveTime", 30, 3, 300);
		overTime = load(element, "thread.overTime", 3600, 300, 86400);
		cancellable = load(element, "thread.cancellable", false); 

		loadExtends();
	}

	boolean isValid() throws Exception {

		refreshScanner();
		return valid();
	}

	@SuppressWarnings("rawtypes")
	void refreshScanner() throws Exception{
		Class<?> clzz = Class.forName(scannerClass);
		Constructor<?> constructor = clzz.getDeclaredConstructor(String.class);
		constructor.setAccessible(true); 
		scanner = (Scanner)constructor.newInstance(name);
	}

	long nextScanTime(){
		Date nextDate = cron.getTimeAfter(new Date());
		return nextDate.getTime() - System.currentTimeMillis();
	}
	
	@Override
	public boolean isDelMatchFailFile() {
		return delMatchFail;
	}

	@Override
	public final boolean matchSrc(String srcName){
		if(pattern == null){
			return true;
		}
		return pattern.matcher(srcName).find();
	}

	/**
	 * 获取config加载的xml
	 * @return
	 */
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
		boolean value = XmlUtil.getBoolean(extendsElement, key, defaultValue);
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

	private Map<String,String> entryMap = new HashMap<>();

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
		builder.append("type=" + getTypeName());
		builder.append("\nvalid=" + valid);
		builder.append("\n" + entryMap);
		return builder.toString();
	}

}
