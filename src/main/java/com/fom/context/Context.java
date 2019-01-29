package com.fom.context;

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
import org.apache.log4j.Logger;
import org.quartz.CronExpression;

import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 * @param <E>
 */
public abstract class Context {

	//所有的Context共用，防止两个Context创建针对同一个文件的任务
	private static Map<String,TimedFuture<Boolean>> futureMap = new ConcurrentHashMap<String,TimedFuture<Boolean>>(100);

	//Context私有线程池，在Context结束时shutdown(),等待任务线程自行响应中断
	private TimedExecutorPool pool = new TimedExecutorPool(4,30,new LinkedBlockingQueue<Runnable>(50));

	static final String CRON = "cron";

	static final String REMARK = "remark";

	static final String THREADCORE = "threadCore";

	static final String THREADMAX = "threadMax";

	static final String ALIVETIME = "threadAliveTime";

	static final String OVERTIME = "threadOverTime";

	static final String CANCELLABLE = "cancellable";

	protected final Logger log;

	protected final String name;

	public Context(){
		Class<?> clazz = this.getClass();
		FomContext fc = clazz.getAnnotation(FomContext.class);
		if(fc == null){
			this.name = clazz.getSimpleName();
			this.log = LoggerFactory.getLogger(name);
			setThreadCore(4);
			setThreadMax(10);
			setAliveTime(30);
			setOverTime(3600);
			setCancellable(false);
		}else{
			if(StringUtils.isBlank(fc.name())){
				this.name = clazz.getSimpleName();
				this.log = LoggerFactory.getLogger(name);
			}else{
				this.name = fc.name();
				this.log = LoggerFactory.getLogger(name);
			}
			setCron(fc.cron());
			setRemark(fc.remark());
			setThreadCore(fc.threadCore());
			setThreadMax(fc.threadMax());
			setAliveTime(fc.threadAliveTime());
			setOverTime(fc.threadOverTime());
			setCancellable(fc.cancellable());
		}
		pool.allowCoreThreadTimeOut(true);
		//在构造器中发布this引用不安全，从ContextManager获取时需要注意
		ContextManager.register(this);
	}

	public Context(String name){
		if(StringUtils.isBlank(name)){
			throw new IllegalArgumentException("param name cann't be empty.");
		}
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
		Class<?> clazz = this.getClass();
		FomContext fc = clazz.getAnnotation(FomContext.class);
		if(fc == null){
			setThreadCore(4);
			setThreadMax(10);
			setAliveTime(30);
			setOverTime(3600);
			setCancellable(false);
		}else{
			setCron(fc.cron());
			setRemark(fc.remark());
			setThreadCore(fc.threadCore());
			setThreadMax(fc.threadMax());
			setAliveTime(fc.threadAliveTime());
			setOverTime(fc.threadOverTime());
			setCancellable(fc.cancellable());
		}
		pool.allowCoreThreadTimeOut(true);
		ContextManager.register(this);
	}

	public Context(String remark, String cron, int threadCore, int threadMax, 
			int threadAliveTime, int threadOverTime, boolean cancellable){
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
		setRemark(remark);
		setCron(cron);
		setThreadCore(threadCore);
		setThreadMax(threadMax);
		setAliveTime(threadAliveTime);
		setOverTime(threadOverTime);
		setCancellable(cancellable);
		ContextManager.register(this);
	}

	public Context(String name, String remark, String cron, int threadCore, int threadMax, 
			int threadAliveTime, int threadOverTime, boolean cancellable){
		if(StringUtils.isBlank(name)){
			throw new IllegalArgumentException("param name cann't be empty.");
		}
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
		setRemark(remark);
		setCron(cron);
		setThreadCore(threadCore);
		setThreadMax(threadMax);
		setAliveTime(threadAliveTime);
		setOverTime(threadOverTime);
		setCancellable(cancellable);
		ContextManager.register(this);
	}

	/**
	 * valueMap只允许put动作，在put时先判断key是否存在，再判断value是否相等，可以很好的避免线程安全问题
	 */
	private Map<String, Object> valueMap = new ConcurrentHashMap<>();

	private volatile CronExpression cronExpression;

	public final String getRemark(){
		return (String)valueMap.get(REMARK);
	}

	public final void setRemark(String remark){
		if(null == valueMap.get(REMARK) 
				|| remark.equals(valueMap.get(REMARK))){ 
			valueMap.put(REMARK, remark);
		}
	}

	public final int getThreadCore(){
		return (int)valueMap.get(THREADCORE);
	}

	public final void setThreadCore(int threadCore){
		if(threadCore < 1 || threadCore > 10){
			threadCore = 4;
		}
		if(null == valueMap.get(THREADCORE) 
				|| threadCore != (int)valueMap.get(THREADCORE)){
			valueMap.put(THREADCORE, threadCore);
		}
	}

	public final int getThreadMax(){
		return (int)valueMap.get(THREADMAX);
	}

	public final void setThreadMax(int threadMax){
		if(threadMax < 10 || threadMax > 100){
			threadMax = 10;
		}
		if(null == valueMap.get(THREADMAX) 
				|| threadMax != (int)valueMap.get(THREADMAX)){
			valueMap.put(THREADMAX, threadMax);
		}
	}

	public final int getAliveTime(){
		return (int)valueMap.get(ALIVETIME);
	}

	public final void setAliveTime(int aliveTime){
		if(aliveTime < 3 || aliveTime > 600){
			aliveTime = 30;
		}
		if(null == valueMap.get(ALIVETIME) 
				|| aliveTime != (int)valueMap.get(ALIVETIME)){
			valueMap.put(ALIVETIME, aliveTime);
		}
	}

	public final int getOverTime(){
		return (int)valueMap.get(OVERTIME);
	}

	public final void setOverTime(int overTime){
		if(overTime < 60 || overTime > 86400){
			overTime = 3600;
		}
		if(null == valueMap.get(OVERTIME) 
				|| overTime != (int)valueMap.get(OVERTIME)){
			valueMap.put(OVERTIME, overTime);
		}
	}

	public final boolean getCancellable(){
		return (boolean)valueMap.get(CANCELLABLE);
	}

	public final void setCancellable(boolean cancellable){
		if(null == valueMap.get(CANCELLABLE) 
				|| cancellable != (boolean)valueMap.get(CANCELLABLE)){
			valueMap.put(CANCELLABLE, cancellable);
		}
	}

	public final String getCron(){
		return (String)valueMap.get(CRON);
	}

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

	public final void stop(){
		if(!state.get()){
			log.warn("context[" + name + "]已经停止"); 
			return;
		}
		log.info("context[" + name + "]停止"); 
		state.set(false); 
	}

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
							futureMap.put(sourceUri, pool.submit(createExecutor(sourceUri))); 
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

	protected abstract List<String> getUriList() throws Exception;

	protected abstract Executor createExecutor(String sourceUri) throws Exception;

	private static final Logger logger = LoggerFactory.getLogger("record");

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
			ExecutionException e1 = null;
			try {
				result = future.get();
			} catch (InterruptedException e) {
				log.warn("cleanFuture interrupted, and recover interrupt flag."); 
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				e1 = e;
			} 
			StringBuilder builder = new StringBuilder(name + "\t" + result +"\t" + "Exception=");
			if(e1 == null){
				builder.append("null");
			}else{
				builder.append(e1.getMessage());
			}
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
}
