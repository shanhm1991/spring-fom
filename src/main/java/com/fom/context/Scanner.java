package com.fom.context;

import java.io.File;
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

import com.fom.context.config.ConfigManager;
import com.fom.context.config.IConfig;
import com.fom.context.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
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

	public Scanner(String name, IConfig config){
		this.name = name;
		this.log = LoggerFactory.getLogger(config.getType() + "." + name);
		this.setName("scanner[" + config.getSrcPath() + "]");
		pool.allowCoreThreadTimeOut(true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public final void run(){
		log.info("启动扫描."); 
		while(true){
			E config = (E)ConfigManager.get(name);
			if(config == null || !config.isRunning()){ 
				log.info("终止扫描."); 
				pool.shutdownNow();
				return;
			}
			Thread.currentThread().setName("scanner[" + config.getSrcPath() + "]");
			if(pool.getCorePoolSize() != config.getExecutorMin()){
				pool.setCorePoolSize(config.getExecutorMin());
			}
			if(pool.getMaximumPoolSize() != config.getExecutorMax()){
				pool.setMaximumPoolSize(config.getExecutorMax());
			}
			if(pool.getKeepAliveTime(TimeUnit.SECONDS) != config.getExecutorAliveTime()){
				pool.setKeepAliveTime(config.getExecutorAliveTime(), TimeUnit.SECONDS);
			}

			cleanFuture(config);

			List<String> fileNameList = filter(config);
			if(fileNameList != null){
				for (String fileName : fileNameList){
					String path = config.getSrcPath() + File.separator + fileName;
					if(isExecutorAlive(fileName)){
						continue;
					}
					try {
						Class<?> clzz = Class.forName(config.getExecutorClass());
						Constructor<?> constructor = clzz.getDeclaredConstructor(String.class, String.class);
						constructor.setAccessible(true);
						Executor executor = (Executor)constructor.newInstance(name, path);
						futureMap.put(path, pool.submit(executor));
						log.info("新建" + config.getTypeName() + "任务" + config.getType() + "[" + fileName + "]"); 
					} catch (RejectedExecutionException e) {
						log.warn(config.getTypeName() + "任务提交被拒绝,等待下次提交[" + fileName + "].");
						break;
					}catch (Exception e) {
						log.error(config.getTypeName() + "任务新建异常[" + fileName + "]", e); 
					}
				}
			}
			synchronized (this) {
				try {
					wait(config.getCronTime());
				} catch (InterruptedException e) {
					log.info("wait interrupted."); 
				}
			}
		}
	}

	public abstract List<String> scan(E config);
	
	public abstract List<String> filter(E config);

	private void cleanFuture(E config){
		long cleanTime = System.currentTimeMillis() / 1000;
		Iterator<Map.Entry<String, TimedFuture<Void>>> it = futureMap.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, TimedFuture<Void>> entry = it.next();
			if(!config.matchSrc(entry.getKey())){
				continue;
			}
			TimedFuture<Void> future = entry.getValue();
			if(future.isDone()){
				it.remove();
			}else{
				long existTime = cleanTime - future.getCreateTime();
				if(existTime > config.getExecutorOverTime()) {
					if(config.getExecutorCancelOnOverTime()){
						future.cancel(true);
						log.warn(config.getTypeName() + "任务超时中断[" + entry.getKey() + "]," + existTime + "s"); 
					}else{
						log.warn(config.getTypeName() + "任务超时[" + entry.getKey() + "]," + existTime + "s");
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
