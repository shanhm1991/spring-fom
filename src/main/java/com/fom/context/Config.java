package com.fom.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.quartz.CronExpression;

import com.fom.context.scanner.Scanner;
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
	
	protected final String name;

	Element element;

	boolean valid;
	
	long loadTime;
	
	long startTime;
	
	volatile boolean isRunning = false;

	protected Config(String name){
		this.name = name;
	}

	public final String toXml(){
		return element.asXML();
	}

	String srcPath;

	String reg;

	boolean delMatchFailFile;

	String scannerClzz;

	String scannerCron;

	String executorClzz;

	int executorMin;

	int executorMax;

	int executorAliveTime;

	int executorOverTime;

	boolean executorCancelOnOverTime;

	void load() throws Exception {
		srcPath = ContextUtil.getEnvStr(XmlUtil.getString(element, "src.path", ""));
		reg = XmlUtil.getString(element, "src.pattern", "");
		delMatchFailFile = XmlUtil.getBoolean(element, "src.match.fail.del", false);
		scannerClzz = XmlUtil.getString(element, "scanner", "");
		scannerCron = XmlUtil.getString(element, "scanner.cron", "");
		executorClzz = XmlUtil.getString(element, "executor", "");
		executorMin = XmlUtil.getInt(element, "executor.min", 4, 1, 10);
		executorMax = XmlUtil.getInt(element, "executor.max", 20, 10, 50);
		executorAliveTime = XmlUtil.getInt(element, "executor.aliveTime.seconds", 30, 3, 300);
		executorOverTime = XmlUtil.getInt(element, "executor.overTime.seconds", 3600, 300, 86400);
		executorCancelOnOverTime = XmlUtil.getBoolean(element, "executor.overTime.cancle", false);
		load(element.element("extended"));
	}

	@SuppressWarnings("rawtypes")
	Scanner scanner;

	Pattern pattern;

	private CronExpression cronExpression;

	boolean isValid() throws Exception {
		if(!StringUtils.isBlank(reg)){
			pattern = Pattern.compile(reg);
		}
		cronExpression = new CronExpression(scannerCron);
		refreshScanner();
		return valid();
	}
	
	@SuppressWarnings("rawtypes")
	void refreshScanner() throws Exception{
		Class<?> clzz = Class.forName(scannerClzz);
		Class<?> pclzz = getParameterType(getClass());
		Constructor<?> constructor = clzz.getDeclaredConstructor(String.class, pclzz);
		constructor.setAccessible(true); 
		scanner = (Scanner)constructor.newInstance(name, this);
	}
	
	private Class<?> getParameterType(Class<?> clzz) {
		Type[] ts = clzz.getGenericInterfaces();
		if(!ArrayUtils.isEmpty(ts)){
			Class<?> clazz = (Class<?>)ts[0];
			clazz.asSubclass(IConfig.class);
			return clazz;
		}
		Class<?> sclzz = (Class<?>)clzz.getGenericSuperclass();
		return getParameterType(sclzz);
	}

	@Override
	public final boolean isRunning(){
		return isRunning;
	}
	
	@Override
	public final boolean isDelMatchFailFile() {
		return delMatchFailFile;
	}
	
	@Override
	public final String getUri() {
		return srcPath;
	}

	@Override
	public final int getExecutorMin() {
		return executorMin;
	}

	@Override
	public final int getExecutorMax() {
		return executorMax;
	}

	@Override
	public final int getExecutorAliveTime() {
		return executorAliveTime;
	}

	@Override
	public final int getExecutorOverTime() {
		return executorOverTime;
	}

	@Override
	public final boolean getInterruptOnOverTime() {
		return executorCancelOnOverTime;
	}

	@Override
	public final String getExecutorClass() {
		return executorClzz;
	}

	public final long nextScanTime(){
		Date nextDate = cronExpression.getTimeAfter(new Date());
		return nextDate.getTime() - System.currentTimeMillis();
	}

	public final boolean matchSrc(String srcName){
		if(pattern == null){
			return true;
		}
		return pattern.matcher(srcName).find();
	}
	
	/**
	 * 子类负责自定义加载<extended>中的配置
	 * @param e
	 * @throws Exception
	 */
	protected void load(Element e) throws Exception {

	}
	
	/**
	 * 子类负责自定义校验<extended>中的配置加载结果，默认返回true
	 * @return
	 * @throws Exception
	 */
	protected boolean valid() throws Exception {
		return true;
	}
	
	//TODO
	@Override
	public boolean equals(Object o){
		if(!(o instanceof Config)){
			return false;
		}
		if(o == this){
			return true;
		}
		Config c = (Config)o;
		return srcPath.equals(c.srcPath)
				&& reg.equals(c.reg)
				&& delMatchFailFile == c.delMatchFailFile
				&& scannerClzz.equals(c.scannerClzz)
				&& scannerCron.equals(c.scannerCron)
				&& executorClzz.equals(c.executorClzz)
				&& executorMin == c.executorMin
				&& executorMax == c.executorMax
				&& executorAliveTime == c.executorAliveTime
				&& executorOverTime == c.executorOverTime
				&& executorCancelOnOverTime == c.executorCancelOnOverTime;
	}
	
	//TODO
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("type=" + getTypeName());
		builder.append("\nvalid=" + valid);
		builder.append("\nsrc.path=" + srcPath);
		builder.append("\nsrc.pattern=" + reg);
		builder.append("\nsrc.match.fail.del=" + delMatchFailFile);
		builder.append("\nscanner=" + scannerClzz);
		builder.append("\nscanner.cron=" + scannerCron);
		builder.append("\nexecutor=" + executorClzz);
		builder.append("\nexecutor.min=" + executorMin);
		builder.append("\nexecutor.max=" + executorMax);
		builder.append("\nexecutor.aliveTime.seconds=" + executorAliveTime);
		builder.append("\nexecutor.overTime.seconds=" + executorOverTime);
		builder.append("\nexecutor.overTime.cancle=" + executorCancelOnOverTime);
		return builder.toString();
	}

}
