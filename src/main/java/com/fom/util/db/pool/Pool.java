package com.fom.util.db.pool;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.dom4j.Element;

import com.fom.util.log.LoggerFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * 连接池管理  维护一个空闲池; 轮询监控关闭超时的空闲连接; 限制最大创建连接数
 * 
 * @author shanhm1991
 *
 * @param <E>
 */
abstract class Pool<E> {

	protected static final Logger LOG = LoggerFactory.getLogger("pool");

	private static AtomicInteger aliveTotal = new AtomicInteger(0);

	private final Queue<Node<E>> freeQueue = new ConcurrentLinkedQueue<Node<E>>(); 

	private final ThreadLocal<Node<E>> threadLocal = new ThreadLocal<Node<E>>();

	private final Object lock = new Object();

	private AtomicInteger aliveCount = new AtomicInteger(0);

	private AtomicInteger freeCount = new AtomicInteger(0);//空闲池中超时关闭的连接不应该被计数

	protected volatile int core = 3; 

	protected volatile int max = 3; 

	protected volatile long overTime = 0;//如果time = 0   1.不做超时关闭检查; 2.获取时如果没有空闲连接永久等待

	protected volatile String name;

	Pool(String name){
		this.name = name;
	}

	protected abstract void load(Element e) throws Exception;

	@SuppressWarnings("unchecked")
	final void clean(){
		Object[] array = freeQueue.toArray();
		if(array == null){
			return;
		}
		for(Object o : array){
			Node<E> node = (Node<E>)o;
			synchronized (node){
				if(!node.isIdel || node.isClosed){
					continue;
				}
				if(System.currentTimeMillis() - node.releaseTime < overTime){
					continue;
				}
				node.isClosed = true;
			}
			freeCount.decrementAndGet();
			aliveCount.decrementAndGet();
			aliveTotal.decrementAndGet();
			node.close();
			LOG.warn("关闭连接[overTime]" + state());
		}
	}

	public Node<E> acquire() throws Exception{  //TODO
		Node<E> node = threadLocal.get();
		if(node != null){
			if(node.isValid()){
				if(!node.isReset()){ //syn(this)
					return node;
				}else{
					LOG.warn("关闭连接[reseted when acquire]" + state()); 
				}
			}else{
				LOG.warn("关闭连接[invalid when acquire]" + state()); 
			}
			node.close();
			aliveCount.decrementAndGet();
			aliveTotal.decrementAndGet();
		}

		synchronized (lock) {
			node = acquireFree();
			if(node != null){
				return node;
			}

			while(aliveCount.get() >= max){
				lock.wait(30000);
				node = acquireFree();
				if(node != null){
					return node;
				}
			}

			node = create();
			threadLocal.set(node);
			aliveCount.incrementAndGet();
			aliveTotal.incrementAndGet();
			LOG.info("获取连接[create]" + state());
		}
		return node;

	}

	private Node<E> acquireFree() { 
		Node<E> node = null;
		while((node = freeQueue.poll()) != null){
			synchronized (node) { 
				if(node.isClosed){
					LOG.warn("忽略连接[closed when acquire free]" + state()); 
					continue;
				}
				node.isIdel = false;
			}

			freeCount.decrementAndGet();
			if(!node.isValid()){
				node.close();
				aliveCount.decrementAndGet();
				aliveTotal.decrementAndGet();
				LOG.warn("关闭连接[invalid when acquire free]" + state()); 
				continue;
			}

			if(node.isReset()){ //syn(this)
				node.close();
				aliveCount.decrementAndGet();
				aliveTotal.decrementAndGet();
				LOG.warn("关闭连接[reseted when acquire free]" + state()); 
				continue;
			}

			threadLocal.set(node);
			LOG.info("获取连接[free]" + state()); 
			return node;
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void release() {
		Node node = threadLocal.get();
		threadLocal.remove();
		if(node == null){
			return;
		}
		
		synchronized (node){
			node.isIdel = true;
			node.isClosed = false;
		}
		node.releaseTime = System.currentTimeMillis();

		if(!node.isValid()){
			node.close();
			aliveCount.decrementAndGet();
			aliveTotal.decrementAndGet();
			LOG.warn("关闭连接[invalid when release]" + state()); 
			return;
		}

		if(node.isReset()){//syn(this)
			node.close();
			aliveCount.decrementAndGet();
			aliveTotal.decrementAndGet();
			LOG.warn("关闭连接[reseted when release]" + state()); 
			return;
		}

		synchronized (lock) { 
			if(freeCount.get() >= core){
				node.close();
				aliveCount.decrementAndGet();
				aliveTotal.decrementAndGet();
				LOG.warn("关闭连接[free full when release]" + state()); 
				return;
			}

			if(!freeQueue.offer(node)){
				node.close();
				aliveCount.decrementAndGet();
				aliveTotal.decrementAndGet();
				LOG.warn("关闭连接[release failed]" + state()); 
				return;
			}

			freeCount.incrementAndGet();
			LOG.info("释放连接" + state()); 
			lock.notifyAll();
		}
	}

	private String state(){
		return ", " + name + "连接池状态[空闲数/总数]：" + freeCount.get() + "/" + aliveCount.get();
	}

	protected abstract Node<E> create() throws Exception;

	final int getLocalAlives(){
		return aliveCount.get();
	}

	static final int getAlives(){
		return aliveTotal.get();
	}

	static final String getString(Element el, String key, String defaultValue){
		Element e = el.element(key);
		if(e == null){
			return defaultValue;
		}

		String value = e.getTextTrim();
		if(StringUtils.isBlank(value)){
			return defaultValue;
		}
		return value;
	}

	static final int getInt(Element el, String key, int defaultValue, int min, int max){
		Element e = el.element(key);
		if(e == null){
			return defaultValue;
		}

		int value = 0;
		try{
			value = Integer.parseInt(e.getTextTrim());
		}catch(Exception e1){
			return defaultValue;
		}

		if(value < min || value > max){
			return defaultValue;
		}
		return value;
	}

	/**
	 * 将同步操作的单元由pool改为node,原先的pool实现要实现动态响应配置变化非常麻烦
	 * 修改之后只需要在acquire()和release()时判断node是否 进行isReset()
	 * 
	 * @author shanhm1991
	 *
	 * @param <T>
	 */
	static abstract class Node<T>{

		boolean isIdel;

		boolean isClosed;

		volatile T v;

		volatile long releaseTime;

		abstract void close();

		abstract boolean isValid();

		abstract boolean isReset();
	}
}
