package com.fom.context;

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

import org.apache.log4j.Logger;
import org.quartz.CronExpression;

import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 * @param <E>
 */
public abstract class Context<E extends Config> {

	private static final Logger logger = LoggerFactory.getLogger("record");

	//所有的Context共用，防止两个Context创建针对同一个文件的任务
	private static Map<String,TimedFuture<Boolean>> futureMap = new ConcurrentHashMap<String,TimedFuture<Boolean>>(100);

	//Context私有线程池，在Context结束时shutdown(),等待任务线程自行响应中断
	private TimedExecutorPool pool = new TimedExecutorPool(4,30,new LinkedBlockingQueue<Runnable>(50));

	private Config config;

	protected Logger log;

	protected final String name;
	
	String remark;
	
	private InnerThread innerThread;
	
	private AtomicBoolean state = new AtomicBoolean(false);

	public Context(){
		FomContext c = this.getClass().getAnnotation(FomContext.class);
		this.name = c.name();
		this.remark = c.remark();
		this.log = LoggerFactory.getLogger(name);
		pool.allowCoreThreadTimeOut(true);
	}

	protected abstract List<String> getUriList(E config) throws Exception;

	protected abstract Executor createExecutor(String sourceUri, E config) throws Exception;

	final void start(){
		if(state.get()){
			log.warn("[" + name + "]已经在运行"); 
			return;
		}
		innerThread = new InnerThread();
		state.set(true); 
		log.info("启动[" + name + "]"); 
		innerThread.start();
	}
	
	final void stop(){
		if(!state.get()){
			log.warn("[" + name + "]已经停止"); 
			return;
		}
		log.info("停止[" + name + "]"); 
		state.set(false); 
	}
	
	final void restart(){
		if(state.get()){
			log.info("重启[" + name + "]"); 
			innerThread.interrupt();
		}else{
			start();
		}
	}

	private class InnerThread extends Thread {
		
		public InnerThread(){
			this.setName(name); 
		}
		
		@Override
		public void run() {
			config = ConfigManager.get(name);
			if(config == null){ 
				log.warn("启动失败,获取config失败."); 
				pool.shutdownNow();
				state.set(false); 
				return;
			}
			while(true){
				if(!state.get()){
					
				}
				config = ConfigManager.get(name);
				if(pool.getCorePoolSize() != config.threadCore){
					pool.setCorePoolSize(config.threadCore);
				}
				if(pool.getMaximumPoolSize() != config.threadMax){
					pool.setMaximumPoolSize(config.threadMax);
				}
				if(pool.getKeepAliveTime(TimeUnit.SECONDS) != config.threadAliveTime){
					pool.setKeepAliveTime(config.threadAliveTime, TimeUnit.SECONDS);
				}

				cleanFuture(config);

				@SuppressWarnings("unchecked")
				E subConfig = (E)config;
				List<String> uriList = null;
				try {
					uriList = getUriList(subConfig);
				} catch (Exception e) {
					log.error("", e); 
				}

				if(uriList != null){
					for (String sourceUri : uriList){
						if(isExecutorAlive(sourceUri)){
							continue;
						}
						try {
							futureMap.put(sourceUri, pool.submit(createExecutor(sourceUri, subConfig))); 
							log.info("新建任务" + "[" + sourceUri + "]"); 
						} catch (RejectedExecutionException e) {
							log.warn("提交任务被拒绝,等待下次提交[" + sourceUri + "].");
							break;
						}catch (Exception e) {
							log.error("新建任务异常[" + sourceUri + "]", e); 
						}
					}
				}
				synchronized (this) {
					CronExpression cron = config.getCron();
					if(cron == null){
						//默认只执行一次，执行完便停止，等待提交的线程结束
						pool.shutdown();
						try {
							pool.awaitTermination(1, TimeUnit.DAYS);
						} catch (InterruptedException e) {
							log.warn("wait interrupted."); 
						}
						cleanFuture(config);
						state.set(false); 
						return;
					}
					Date nextTime = cron.getTimeAfter(new Date());
					long waitTime = nextTime.getTime() - System.currentTimeMillis();
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

	private void cleanFuture(Config config){
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
				if(existTime > config.threadOverTime) {
					log.warn("任务超时[" + sourceUri + "]," + existTime + "s");
					if(config.cancellable){
						future.cancel(true);
					}
				}
				continue;
			}

			it.remove();
			boolean result = true;
			ExecutionException e1 = null;
			Exception e2 = null;
			try {
				result = future.get();
				future.callback(result);
			} catch (InterruptedException e) {
				log.warn("cleanFuture interrupted, and recover interrupt flag."); 
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				e1 = e;
			} catch(Exception e){
				log.error("回调执行异常", e); 
				e2 = e;
			}
			StringBuilder builder = new StringBuilder(name + "\t" + result +"\t" + "ExecutionException=");
			if(e1 == null){
				builder.append("null");
			}else{
				builder.append(e1.getMessage());
			}
			builder.append("\t" + "CallbackException=");
			if(e2 == null){
				builder.append("null");
			}else{
				builder.append(e2.getMessage());
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
