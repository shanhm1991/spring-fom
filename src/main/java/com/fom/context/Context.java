package com.fom.context;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hdfs.server.namenode.UnsupportedActionException;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dom4j.Element;
import org.quartz.CronExpression;

import com.fom.log.LoggerAppender;
import com.fom.log.LoggerFactory;
import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 *
 * @param <E>
 */
public abstract class Context {

	//所有的Context共用，防止两个Context创建针对同一个文件的任务
	private static Map<String,TimedFuture<Boolean>> futureMap = new ConcurrentHashMap<String,TimedFuture<Boolean>>(100);

	//容器启动时会给elementMap赋值，Context构造时尝试从中获取配置
	static final Map<String, Element> elementMap = new ConcurrentHashMap<>();

	static final String CRON = "cron";

	static final String REMARK = "remark";
	
	static final String QUEUESIZE = "queueSize";

	static final String THREADCORE = "threadCore";

	static final String THREADMAX = "threadMax";

	static final String ALIVETIME = "threadAliveTime";

	static final String OVERTIME = "threadOverTime";

	static final String CANCELLABLE = "cancellable";
	
	protected final Logger log;

	protected final String name;

	//Context私有线程池，在Context结束时shutdown(),等待任务线程自行响应中断
	private TimedExecutorPool pool;

	public Context(){
		Class<?> clazz = this.getClass();
		FomContext fc = clazz.getAnnotation(FomContext.class);
		if(fc == null){
			this.name = clazz.getSimpleName();
			this.log = LoggerFactory.getLogger(name);
		}else{
			if(StringUtils.isBlank(fc.name())){
				this.name = clazz.getSimpleName();
				this.log = LoggerFactory.getLogger(name);
			}else{
				this.name = fc.name();
				this.log = LoggerFactory.getLogger(name);
			}
		}
		initValue(name, fc);
	}

	public Context(String name){
		if(StringUtils.isBlank(name)){
			throw new IllegalArgumentException("param name cann't be empty.");
		}
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
		Class<?> clazz = this.getClass();
		FomContext fc = clazz.getAnnotation(FomContext.class);
		initValue(name, fc);
	}
	
	/**
	 * xml > 注解  > 默认
	 * @param name
	 * @param fc
	 */
	private void initValue(String name,FomContext fc){
		int core = 4; 
		int max = 20;
		int aliveTime = 30;
		int overTime = 3600; 
		int queueSize = 200;
		Element element = elementMap.get(name); 
		if(element != null){
			core = setThreadCore(XmlUtil.getInt(element, THREADCORE, 4, 1, 10));
			max = setThreadMax(XmlUtil.getInt(element, THREADMAX, 20, 10, 100));
			aliveTime = setAliveTime(XmlUtil.getInt(element, ALIVETIME, 30, 5, 300));
			overTime = setOverTime(XmlUtil.getInt(element, OVERTIME, 3600, 60, 86400));
			queueSize = setQueueSize(XmlUtil.getInt(element, QUEUESIZE, 200, 1, 10000000));
			setCancellable(XmlUtil.getBoolean(element, CANCELLABLE, false));
			setCron(XmlUtil.getString(element, CRON, ""));
			setRemark(XmlUtil.getString(element, REMARK, ""));
			loadconfigs(element);
		}else if(fc != null){
			core = setThreadCore(fc.threadCore());
			max = setThreadMax(fc.threadMax());
			aliveTime = setAliveTime(fc.threadAliveTime());
			overTime = setOverTime(fc.threadOverTime());
			queueSize = setQueueSize(fc.queueSize());
			setCancellable(fc.cancellable());
			setCron(fc.cron());
			setRemark(fc.remark());
		}else{
			setThreadCore(core);
			setThreadMax(max);
			setAliveTime(aliveTime);
			setOverTime(overTime);
			setQueueSize(queueSize);
			setCancellable(false);
		}
		pool = new TimedExecutorPool(core,max,aliveTime,new LinkedBlockingQueue<Runnable>(queueSize));
		pool.allowCoreThreadTimeOut(true);
	}
	
	@SuppressWarnings("unchecked")
	private void loadconfigs(Element element){
		List<Element> list = element.elements();
		for(Element e : list){
			String name = e.getName();
			if(!validKey(name)){
				continue;
			}
			valueMap.put(name, e.getTextTrim());
		}
	}
	
	private int setQueueSize(int queueSize){
		if(queueSize < 1 || queueSize > 10000000){
			queueSize = 200;
		}
		if(null == valueMap.get(QUEUESIZE) 
				|| queueSize != (int)valueMap.get(QUEUESIZE)){
			valueMap.put(QUEUESIZE, queueSize);
		}
		return queueSize;
	}
	
	private boolean validKey(String key){
		boolean valid = THREADCORE.equals(key) || THREADMAX.equals(key) 
				|| ALIVETIME.equals(key) || OVERTIME.equals(key) || QUEUESIZE.equals(key)
				|| CANCELLABLE.equals(key) || CRON.equals(key) || REMARK.equals(key);
		return !valid;
	}
	
	/**
	 * 将key-value保存到valueMap中,可以在getValue或其他get中获取
	 * @param key key
	 * @param value value
	 * @throws UnsupportedActionException
	 */
	public final void setValue(String key, Object value) throws UnsupportedActionException{
		if(!validKey(key)){
			throw new UnsupportedActionException("不支持设置的key:" + key);
		}
		valueMap.put(key, value);
	}
	
	/**
	 * 通过key获取valueMap中的值
	 * @param Object
	 * @return
	 */
	public final Object getValue(String key){
		return valueMap.get(key);
	}
	
	/**
	 * 获取valueMap中int值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return
	 */
	public final int getInt(String key, int defaultValue){
		try{
			return Integer.parseInt(String.valueOf(valueMap.get(key)));
		}catch(Exception e){
			return defaultValue;
		}
	}
	
	/**
	 * 获取valueMap中long值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return
	 */
	public final long getLong(String key, long defaultValue){
		try{
			return Long.parseLong(String.valueOf(valueMap.get(key)));
		}catch(Exception e){
			return defaultValue;
		}
	}
	
	/**
	 * 获取valueMap中boolean值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return
	 */
	public final boolean getBoolean(String key, boolean defaultValue){
		try{
			return Boolean.parseBoolean(String.valueOf(valueMap.get(key)));
		}catch(Exception e){
			return defaultValue;
		}
	}
	
	/**
	 * 获取valueMap中string值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return
	 */
	public final String getString(String key, String defaultValue){
		String value = String.valueOf(valueMap.get(key));
		if(StringUtils.isBlank(value)){
			return defaultValue;
		}
		return value;
	}

	/**
	 * valueMap只允许put动作，在put时先判断key是否存在，再判断value是否相等，可以很好的避免线程安全问题
	 */
	private Map<String, Object> valueMap = new ConcurrentHashMap<>();

	private volatile CronExpression cronExpression;

	/**
	 * 获取当前Context对象的name
	 * @return
	 */
	public final String getName(){
		return name;
	}

	/**
	 * 获取remark备注信息
	 * @return
	 */
	public final String getRemark(){
		return (String)valueMap.get(REMARK);
	}

	/**
	 * 设置remark备注信息
	 * @return
	 */
	public final void setRemark(String remark){
		if(null == valueMap.get(REMARK) 
				|| remark.equals(valueMap.get(REMARK))){ 
			valueMap.put(REMARK, remark);
		}
	}

	/**
	 * 获取本地线程池的核心线程数
	 * @return
	 */
	public final int getThreadCore(){
		return (int)valueMap.get(THREADCORE);
	}

	/**
	 * 设置本地线程池的核心线程数，将在下一个周期生效
	 * @param threadCore
	 * @return
	 */
	public final int setThreadCore(int threadCore){
		if(threadCore < 1 || threadCore > 10){
			threadCore = 4;
		}
		if(null == valueMap.get(THREADCORE) 
				|| threadCore != (int)valueMap.get(THREADCORE)){
			valueMap.put(THREADCORE, threadCore);
		}
		return threadCore;
	}

	/**
	 * 获取本地线程池的最大线程数
	 * @return
	 */
	public final int getThreadMax(){
		return (int)valueMap.get(THREADMAX);
	}

	/**
	 * 设置本地线程池的最大线程数，将在下一个周期生效
	 * @param threadCore
	 * @return
	 */
	public final int setThreadMax(int threadMax){
		if(threadMax < 10 || threadMax > 100){
			threadMax = 20;
		}
		if(null == valueMap.get(THREADMAX) 
				|| threadMax != (int)valueMap.get(THREADMAX)){
			valueMap.put(THREADMAX, threadMax);
		}
		return threadMax;
	}

	/**
	 * 获取本地线程池的线程存活时间
	 * @return
	 */
	public final int getAliveTime(){
		return (int)valueMap.get(ALIVETIME);
	}

	/**
	 * 设置本地线程池的线程存活时间，将在下一个周期生效
	 * @return
	 */
	public final int setAliveTime(int aliveTime){
		if(aliveTime < 3 || aliveTime > 600){
			aliveTime = 30;
		}
		if(null == valueMap.get(ALIVETIME) 
				|| aliveTime != (int)valueMap.get(ALIVETIME)){
			valueMap.put(ALIVETIME, aliveTime);
		}
		return aliveTime;
	}

	/**
	 * 获取任务线程的超时时间
	 * @return
	 */
	public final int getOverTime(){
		return (int)valueMap.get(OVERTIME);
	}

	/**
	 * 设置任务线程的超时时间，将在下一个周期生效
	 * @return
	 */
	public final int setOverTime(int overTime){
		if(overTime < 60 || overTime > 86400){
			overTime = 3600;
		}
		if(null == valueMap.get(OVERTIME) 
				|| overTime != (int)valueMap.get(OVERTIME)){
			valueMap.put(OVERTIME, overTime);
		}
		return overTime;
	}

	/**
	 * 获取cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @return
	 */
	public final boolean getCancellable(){
		return (boolean)valueMap.get(CANCELLABLE);
	}

	/**
	 * 设置cancellable：决定任务线程在执行时间超过overTime时是否中断
	 * @return
	 */
	public final boolean setCancellable(boolean cancellable){
		if(null == valueMap.get(CANCELLABLE) 
				|| cancellable != (boolean)valueMap.get(CANCELLABLE)){
			valueMap.put(CANCELLABLE, cancellable);
		}
		return cancellable;
	}

	/**
	 * 获取context定时表达式
	 * @return
	 */
	public final String getCron(){
		return (String)valueMap.get(CRON);
	}

	/**
	 * 设置context定时表达式，将在下一个周期生效
	 * @return
	 */
	public final void setCron(String cron){
		if(StringUtils.isBlank(cron)){
			return;
		}
		CronExpression c = null;
		try {
			c = new CronExpression(cron);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		if(null == valueMap.get(CRON) 
				|| !cron.equals((String)valueMap.get(CRON))){
			valueMap.put(CRON, cron);
			cronExpression = c;
		}
	}

	/**
	 * 启动context
	 */
	public final void start(){
		if(state.get()){
			log.warn("context[" + name + "]已经在运行"); 
			return;
		}
		innerThread = new InnerThread();
		state.set(true); 
		log.info("context[" + name + "]启动"); 
		innerThread.start();
	}

	/**
	 * 停止context
	 */
	public final void stop(){
		if(!state.get()){
			log.warn("context[" + name + "]已经停止"); 
			return;
		}
		log.info("context[" + name + "]停止"); 
		state.set(false); 
	}

	/**
	 * 中断context
	 */
	public final void interrupt(){
		if(state.get()){
			log.info("context[" + name + "]重启"); 
			innerThread.interrupt();
		}else{
			start();
		}
	}

	private InnerThread innerThread;

	private AtomicBoolean state = new AtomicBoolean(false);

	private class InnerThread extends Thread {

		public InnerThread(){
			this.setName(name); 
		}

		@Override
		public void run() {
			while(true){
				if(!state.get()){
					pool.shutdownNow(); //TODO
					return;
				}
				int threadCore = (int)(valueMap.get(THREADCORE)); 
				if(pool.getCorePoolSize() != threadCore){
					pool.setCorePoolSize(threadCore);
				}

				int threadMax = (int)(valueMap.get(THREADMAX)); 
				if(pool.getMaximumPoolSize() != threadMax){
					pool.setMaximumPoolSize(threadMax);
				}

				int threadAliveTime = (int)(valueMap.get(ALIVETIME)); 
				if(pool.getKeepAliveTime(TimeUnit.SECONDS) != threadAliveTime){
					pool.setKeepAliveTime(threadAliveTime, TimeUnit.SECONDS);
				}

				cleanFuture();

				List<String> uriList = null;
				try {
					uriList = getUriList();
				} catch (Exception e) {
					log.error("", e); 
				}

				if(uriList != null){
					for (String sourceUri : uriList){
						if(isExecutorAlive(sourceUri)){
							continue;
						}
						try {
							Executor executor = createExecutor(sourceUri);
							executor.setName(name); 
							futureMap.put(sourceUri, pool.submit(executor)); 
							log.info("新建任务" + "[" + sourceUri + "]"); 
						} catch (RejectedExecutionException e) {
							log.warn("提交任务被拒绝,等待下次提交[" + sourceUri + "].");
							break;
						}catch (Exception e) {
							log.error("新建任务异常[" + sourceUri + "]", e); 
						}
					}
				}
				if(cronExpression == null){
					//默认只执行一次，执行完便停止，等待提交的线程结束
					pool.shutdown();
					try {
						pool.awaitTermination(1, TimeUnit.DAYS);
					} catch (InterruptedException e) {
						log.warn("wait interrupted."); 
					}
					cleanFuture();
					state.set(false); 
					log.info("context[" + name + "]结束");
					return;
				}
				Date nextTime = cronExpression.getTimeAfter(new Date());
				long waitTime = nextTime.getTime() - System.currentTimeMillis();
				synchronized (this) {
					try {
						wait(waitTime);
					} catch (InterruptedException e) {
						//借助interrupted标记来重启
						log.info("wait interrupted."); 
					}
				}
			}
		}
	}

	/**
	 * 返回资源uri列表，context将根据每个uri创建一个Executor执行器提交到线程池
	 * @return List<String>
	 * @throws Exception
	 */
	protected abstract List<String> getUriList() throws Exception;

	/**
	 * 根据uri创建一个Executor的具体实例
	 * @param sourceUri 资源uri
	 * @return
	 * @throws Exception
	 */
	protected abstract Executor createExecutor(String sourceUri) throws Exception;

	private void cleanFuture(){
		Iterator<Map.Entry<String, TimedFuture<Boolean>>> it = futureMap.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, TimedFuture<Boolean>> entry = it.next();
			TimedFuture<Boolean> future = entry.getValue();
			if(!name.equals(future.getName())){   
				continue;
			}

			String sourceUri = entry.getKey();
			if(!future.isDone()){
				long existTime = (System.currentTimeMillis() - future.getCreateTime()) / 1000;

				int threadOverTime = (int)(valueMap.get(OVERTIME)); 
				if(existTime > threadOverTime) {
					log.warn("任务超时[" + sourceUri + "]," + existTime + "s");
					if((boolean)valueMap.get(CANCELLABLE)) { 
						future.cancel(true);
					}
				}
				continue;
			}

			it.remove();
			boolean result = true;
			Throwable te = null;
			try {
				result = future.get();
			} catch (InterruptedException e) {
				log.warn("cleanFuture interrupted, and recover interrupt flag."); 
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				log.error("", e); 
			} 
			te = future.getThrowable();
			StringBuilder builder = new StringBuilder("sourceUri=" +sourceUri 
					+ ", result=" + result 
					+ ", creatTime=" + future.getCreateTime()
					+ ", costTime=" + future.getCost());
			if(te == null){
				builder.append(", Exception=null");
			}else{
				builder.append(", Exception=" + te.getMessage());
			}
			Logger logger = getRecoder(name + ".statistic");
			logger.error(builder.toString()); 
		}
	}

	/**
	 * null 没有创建过任务
	 * done 创建过任务，但远程文件没删除
	 * else 任务还没结束
	 */
	private boolean isExecutorAlive(String key){
		Future<Boolean> future = futureMap.get(key);
		return future != null && !future.isDone();
	}
	
	private static Logger getRecoder(String name){
		Logger logger = LogManager.exists(name);
		if(logger != null){
			return logger;
		}
		logger = Logger.getLogger(name); 
		logger.setLevel(Level.INFO);  
		logger.setAdditivity(false); 
		logger.removeAllAppenders();
		LoggerAppender appender = new LoggerAppender();
		PatternLayout layout = new PatternLayout();  
		layout.setConversionPattern("%m%n");  
		appender.setLayout(layout); 
		appender.setEncoding("UTF-8");
		appender.setAppend(true);
		appender.setFile(System.getProperty("log.root") + File.separator + name);
		appender.setRolling("false"); 
		appender.activateOptions();
		logger.addAppender(appender);  
		return logger;
	}
}
