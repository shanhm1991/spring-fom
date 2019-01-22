package com.fom.db.pool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.fom.log.LoggerFactory;
import com.fom.util.XmlUtil;

/**
 * 负责监听配置变化,更新和提高poolMap中的pool
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
final class PoolManager {

	protected static final Logger LOG = LoggerFactory.getLogger("pool");

	private static final ConcurrentMap<String,Pool<?>> poolMap = new ConcurrentHashMap<String,Pool<?>>();

	private static final List<Pool<?>> poolRemoved = new ArrayList<Pool<?>>();

	private static AtomicInteger removeCount = new AtomicInteger(0);

	private static int cleanTimes = 0;

	protected static void listen(File poolXml){
		load(poolXml);//确保在加载启动任务前已经加载过pool
		new Listener(poolXml).start();
		new Monitor().start();
	}
	
	static Pool<?> get(String poolName) {
		return poolMap.get(poolName);
	}
	
	private static void remve(String name){
		Pool<?> pool = poolMap.remove(name);
		if(pool != null){
			pool.name = "removed-" + removeCount.incrementAndGet() + "-" + name;
			LOG.warn("卸载:" + name + ",重命名为:" + pool.name); 
			poolRemoved.add(pool);
		}
	}

	private static void remveAll(){
		Iterator<String> it = poolMap.keySet().iterator();
		while(it.hasNext()){
			String name = it.next();
			remve(name);
		}
	}
	
	/**
	 * Listener单线程调用
	 * @param file
	 */
	@SuppressWarnings("rawtypes")
	private static void load(File file){
		SAXReader reader = new SAXReader();
		reader.setEncoding("UTF-8");
		LOG.info("------------------------------加载连接池------------------------------"); 
		Document doc = null;
		try{
			doc = reader.read(new FileInputStream(file));
		}catch(Exception e){
			LOG.error("加载失败", e); 
			remveAll();
			LOG.info("------------------------------加载结束------------------------------"); 
			return;
		}

		Element pools = doc.getRootElement();
		Iterator it = pools.elementIterator("pool");

		Set<String> nameSet = new HashSet<String>();
		while (it.hasNext()) {
			Element ePool = (Element) it.next();

			String name = XmlUtil.getString(ePool, "name", null);
			if(name == null){
				LOG.warn("忽略没有name的pool"); 
				continue;
			}

			if(nameSet.contains(name)){
				LOG.warn("忽略重名的pool[" + name + "]"); 
				continue;
			}

			String className = XmlUtil.getString(ePool, "class", null);
			if(className == null){
				LOG.warn(name + "加载失败,缺少配置class"); 
				remve(name);
				continue;
			}

			Pool pool = poolMap.get(name);
			if(pool != null){
				if(pool.getClass().getName().equals(className)){
					try {
						pool.load(ePool);
						nameSet.add(name);
					} catch (Exception e) {
						LOG.error(name + "加载失败", e); 
						remve(name);
					}
					continue;
				}else{
					remve(name);
				}
			}

			try {
				Class<?> poolClass = Class.forName(className);
				Constructor ct = poolClass.getDeclaredConstructor(String.class);
				ct.setAccessible(true);
				pool = (Pool)ct.newInstance(name);
				pool.load(ePool);
				nameSet.add(name);
				poolMap.put(name, pool);
			} catch (Exception e) {
				LOG.error(name + "加载失败", e); 
			}
		}
		LOG.info("连接池列表=" + poolMap.keySet());
		LOG.info("------------------------------加载结束------------------------------"); 
	}

	private static class Listener extends Thread {

		private final File poolXml;

		public Listener(File poolXml) {
			this.setName("pool-listener");
			this.setDaemon(true); 
			this.poolXml = poolXml;
		}

		@Override
		public void run() {
			String parentPath = poolXml.getParent();
			String name = poolXml.getName();

			WatchService watch = null;
			try {
				watch = FileSystems.getDefault().newWatchService();
				Paths.get(parentPath).register(watch, StandardWatchEventKinds.ENTRY_MODIFY); 
			} catch (IOException e) {
				LOG.error("配置监听启动失败", e); 
			}

			WatchKey key = null;
			while(true){
				try {
					key = watch.take();
				} catch (InterruptedException e) {
					return;
				}
				for(WatchEvent<?> event : key.pollEvents()){
					if(StandardWatchEventKinds.ENTRY_MODIFY == event.kind()){ 
						String eventName = event.context().toString();
						if(name.equals(eventName)){ 
							load(poolXml);
						}
					}
				}
				key.reset();
			}
		}
	}

	private static class Monitor extends Thread {

		public Monitor() {
			this.setName("pool-monitor");
			this.setDaemon(true); 
		}

		@Override
		public void run() {
			while(true){
				try {
					sleep(15 * 1000); //15s
				} catch (InterruptedException e) {
					//should never happened
				}

				Iterator<Pool<?>> it = poolRemoved.iterator();
				while(it.hasNext()){
					Pool<?> pool = it.next();
					pool.clean();
					if(pool.getLocalAlives() == 0){
						it.remove();
					}
				}

				for(Pool<?> pool : poolMap.values()){
					if(pool.aliveTimeOut == 0){
						continue;
					}
					pool.clean();
				}

				StringBuilder builder = new StringBuilder(", detail=[");
				for(Pool<?> pool : poolMap.values()){
					builder.append(pool.name + ":" + pool.getLocalAlives() + "; ");
				}
				for(Pool<?> pool : poolRemoved){
					builder.append(pool.name + "(removed):" + pool.getLocalAlives() + "; ");
				}
				String detail = builder.append("]").toString();
				if(cleanTimes++ % 10 == 0){
					LOG.info("[统计]当前所有连接数：" + Pool.getAlives() + detail); 
				}
			}
		}
	}
	
}
