package com.fom.context;

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

import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 * @param <E>
 */
public abstract class Context<E extends Config> extends Thread {

	//所有的Context共用，防止两个Context创建针对同一个文件的任务
	private static Map<String,TimedFuture<Boolean>> futureMap = new ConcurrentHashMap<String,TimedFuture<Boolean>>(100);

	//Context私有线程池，在Context结束时shutdown(),等待任务线程自行响应中断
	private TimedExecutorPool pool = new TimedExecutorPool(4,30,new LinkedBlockingQueue<Runnable>(50));

	protected final Logger log;

	protected final String name;

	protected Context(String name){
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
		pool.allowCoreThreadTimeOut(true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void run(){
		log.info(name + "启动"); 
		while(true){
			Config config = ConfigManager.get(name);
			if(config == null || !config.isRunning){ 
				log.info(name + "终止."); 
				pool.shutdownNow();
				return;
			}
			this.setName("[" + config.srcUri + "]");
			if(pool.getCorePoolSize() != config.core){
				pool.setCorePoolSize(config.core);
			}
			if(pool.getMaximumPoolSize() != config.max){
				pool.setMaximumPoolSize(config.max);
			}
			if(pool.getKeepAliveTime(TimeUnit.SECONDS) != config.aliveTime){
				pool.setKeepAliveTime(config.aliveTime, TimeUnit.SECONDS);
			}

			cleanFuture(config);

			E subConfig = (E)config;
			List<String> uriList = null;
			try {
				uriList = scan(config.srcUri, subConfig);
			} catch (Exception e) {
				log.error("扫描异常", e); 
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
				try {
					wait(config.nextScanTime());
				} catch (InterruptedException e) {
					log.info("wait interrupted."); 
				}
			}
		}
	}

	protected abstract List<String> scan(String srcUri, E config) throws Exception;

	protected abstract Executor createExecutor(String sourceUri, E config) throws Exception;

	private void cleanFuture(Config config){
		Iterator<Map.Entry<String, TimedFuture<Boolean>>> it = futureMap.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, TimedFuture<Boolean>> entry = it.next();
			if(!config.matchSource(entry.getKey())){
				continue;
			}
			
			TimedFuture<Boolean> future = entry.getValue();
			if(future.isDone()){
				it.remove();
				boolean result = true;
				try {
					result = future.get();
				} catch (InterruptedException e) {
					//never happened
				} catch (ExecutionException e) {
					e.printStackTrace(); //TODO
				}
				Executor exec = future.getExecutor();
				if(exec != null){
					try{
						exec.callback(result);
					}catch(Exception e){
						log.error("callback异常", e); 
					}
				}
			}else{
				long existTime = (System.currentTimeMillis() - future.getCreateTime()) / 1000;
				if(existTime > config.overTime) {
					log.warn("任务超时[" + entry.getKey() + "]," + existTime + "s");
					if(config.cancellable){
						future.cancel(true);
					}
				}
			}
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
