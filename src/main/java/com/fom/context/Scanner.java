package com.fom.context;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.fom.context.scanner.TimedExecutorPool;
import com.fom.context.scanner.TimedFuture;
import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 * @param <E>
 */
public abstract class Scanner<E extends IConfig> extends Thread {

	protected final Logger log;

	protected final String name;

	//所有的scanner共用，防止两个scanner创建针对同一个文件的任务
	private static Map<String,TimedFuture<Void>> futureMap = new ConcurrentHashMap<String,TimedFuture<Void>>(100);

	//scanner私有线程池，在Scanner结束时shutdown(),等待任务线程自行响应中断
	private TimedExecutorPool pool = new TimedExecutorPool(4,30,new LinkedBlockingQueue<Runnable>(50));

	protected Scanner(String name){
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
		pool.allowCoreThreadTimeOut(true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public final void run(){
		log.info("启动扫描."); 
		while(true){
			Config config = ConfigManager.get(name);
			if(config == null || !config.isRunning){ 
				log.info("终止扫描."); 
				pool.shutdownNow();
				return;
			}
			this.setName("scanner[" + config.srcUri + "]");
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
			List<String> uriList = scan(config.srcUri, subConfig);
			if(uriList != null){
				for (String uri : uriList){
					if(isExecutorAlive(uri)){
						continue;
					}
					try {
						Class<?> clzz = Class.forName(config.contextClass);
						Constructor<?> constructor = clzz.getDeclaredConstructor(String.class, String.class);
						constructor.setAccessible(true);
						Context context = (Context)constructor.newInstance(name, uri);
						futureMap.put(uri, pool.submit(context)); 
						log.info("新建任务" + "[" + uri + "]"); 
					} catch (RejectedExecutionException e) {
						log.warn("任务提交被拒绝,等待下次提交[" + uri + "].");
						break;
					}catch (Exception e) {
						log.error("任务新建异常[" + uri + "]", e); 
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

	/**
	 * 根据srcUri和config返回需要处理的文件路径(绝对路径)列表
	 * @param srcUri
	 * @param config
	 * @return
	 */
	public abstract List<String> scan(String srcUri, E config);
	
	private void cleanFuture(Config config){
		Iterator<Map.Entry<String, TimedFuture<Void>>> it = futureMap.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, TimedFuture<Void>> entry = it.next();
			if(!config.matchSourceName(entry.getKey())){
				continue;
			}
			TimedFuture<Void> future = entry.getValue();
			if(future.isDone()){
				it.remove();
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
		Future<Void> future = futureMap.get(key);
		return future != null && !future.isDone();
	}
}
