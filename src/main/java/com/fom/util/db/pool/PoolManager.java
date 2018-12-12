package com.fom.util.db.pool;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
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

import com.fom.util.log.LoggerFactory;

/**
 * Listener负责监听配置变化和更新poolMap中的pool,Impoter从poolMap中获取使用pool
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
public class PoolManager {

	protected static final Logger LOG = LoggerFactory.getLogger("pool");

	private static final ConcurrentMap<String,Pool<?>> poolMap = new ConcurrentHashMap<String,Pool<?>>();

	private static final List<Pool<?>> poolRemoved = new ArrayList<Pool<?>>();

	private static AtomicInteger removeCount = new AtomicInteger(0);

	private static int cleanTimes = 0;

	//将pool相关的类分离出来一个package，但是又不希望被其他外部类访问到，所以将构造方法隐藏，只给子类调用
	protected PoolManager(){

	}

	static{
		Thread monitor = new Thread("pool-monitor"){
			@Override
			public void run(){
				while(true){
					try {
						sleep(30 * 1000); //30s
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
						if(pool.overTime == 0){
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
					if(cleanTimes++ % 30 == 0){
						LOG.info("[统计]当前所有连接数：" + Pool.getAlives() + detail); 
					}
				}
			}
		};
		monitor.setDaemon(true); 
		monitor.start();
	}

	/**
	 * Listener单线程调用
	 * @param file
	 */
	@SuppressWarnings("rawtypes")
	protected static final void load(String file){
		SAXReader reader = new SAXReader();
		reader.setEncoding("UTF-8");
		LOG.info("------------------------------加载连接池------------------------------"); 
		Document doc = null;
		try{
			doc = reader.read(new FileInputStream(new File(file)));
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

			String name = Pool.getString(ePool, "name", null);
			if(name == null){
				LOG.warn("忽略没有name的pool"); 
				continue;
			}

			if(nameSet.contains(name)){
				LOG.warn("忽略重名的pool[" + name + "]"); 
				continue;
			}

			String className = Pool.getString(ePool, "class", null);
			if(className == null){
				LOG.warn(name + "加载失败,缺少配置class"); 
				remvePool(name);
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
						remvePool(name);
					}
					continue;
				}else{
					remvePool(name);
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

	private static void remvePool(String name){
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
			remvePool(name);
		}
	}

	static final Pool<?> getPool(String poolName) {
		return poolMap.get(poolName);
	}
}
