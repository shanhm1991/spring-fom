package com.fom.db.pool;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.dom4j.Element;

/**
 * 连接池管理  维护一个空闲池; 轮询监控关闭超时的空闲连接; 限制最大创建连接数; 动态响应配置变化
 * 
 * @author shanhm
 *
 * @param <E>
 */
abstract class Pool<E> {

	protected static final Logger LOG = Logger.getLogger(Pool.class);

	private static AtomicInteger aliveTotal = new AtomicInteger(0);

	private final Queue<Node<E>> freeQueue = new ConcurrentLinkedQueue<Node<E>>(); 

	private final ThreadLocal<Node<E>> threadLocal = new ThreadLocal<Node<E>>();

	private final Lock lock = new ReentrantLock();

	private final Condition condition = lock.newCondition();

	private AtomicInteger aliveCount = new AtomicInteger(0);

	private AtomicInteger freeCount = new AtomicInteger(0);//空闲池中超时关闭的连接不应该被计数

	protected volatile int core = 3; 

	protected volatile int max = 3; 

	protected volatile long waitTimeOut = 30000;

	protected volatile long aliveTimeOut = 0;//如果time = 0   1.不做超时关闭检查; 2.获取时如果没有空闲连接永久等待

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
				if(System.currentTimeMillis() - node.releaseTime < aliveTimeOut){
					continue;
				}
				node.isClosed = true;
			}
			freeCount.decrementAndGet();
			aliveCount.decrementAndGet();
			aliveTotal.decrementAndGet();
			node.close();
			if (LOG.isDebugEnabled()) {
				LOG.debug("关闭连接[overTime]" + state());
			}
		}
	}

	public Node<E> acquire() throws Exception{
		Node<E> node = threadLocal.get();
		if(node != null){
			if(node.isValid()){
				if(!node.isReset()){ //syn(this)
					return node;
				}else{
					if (LOG.isDebugEnabled()) {
						LOG.debug("关闭连接[reseted when acquire]" + state()); 
					}
				}
			}else{
				if (LOG.isDebugEnabled()) {
					LOG.debug("关闭连接[invalid when acquire]" + state()); 
				}
			}
			node.close();
			aliveCount.decrementAndGet();
			aliveTotal.decrementAndGet();
		}

		lock.lock();
		try{
			node = acquireFree();
			if(node != null){
				return node;
			}

			while(aliveCount.get() >= max){
				if(!condition.await(waitTimeOut, TimeUnit.MILLISECONDS)){
					throw new Exception("获取连接超时" + state());
				}
				node = acquireFree();
				if(node != null){
					return node;
				}
			}
		}finally{
			lock.unlock();
		}

		node = create();
		threadLocal.set(node);
		aliveCount.incrementAndGet();
		aliveTotal.incrementAndGet();
		if (LOG.isDebugEnabled()) {
			LOG.debug("获取连接[create]" + state());
		}
		return node;
	}



	private Node<E> acquireFree() { 
		Node<E> node = null;
		while((node = freeQueue.poll()) != null){
			synchronized (node) { 
				if(node.isClosed){
					if (LOG.isDebugEnabled()) {
						LOG.debug("忽略连接[closed when acquire free]" + state()); 
					}
					continue;
				}
				node.isIdel = false;
			}

			freeCount.decrementAndGet();
			if(!node.isValid()){
				node.close();
				aliveCount.decrementAndGet();
				aliveTotal.decrementAndGet();
				if (LOG.isDebugEnabled()) {
					LOG.debug("关闭连接[invalid when acquire free]" + state()); 
				}
				continue;
			}

			if(node.isReset()){ //syn(this)
				node.close();
				aliveCount.decrementAndGet();
				aliveTotal.decrementAndGet();
				if (LOG.isDebugEnabled()) {
					LOG.debug("关闭连接[reseted when acquire free]" + state()); 
				}
				continue;
			}

			threadLocal.set(node);
			if (LOG.isDebugEnabled()) {
				LOG.debug("获取连接[free]" + state()); 
			}
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
			if (LOG.isDebugEnabled()) {
				LOG.debug("关闭连接[invalid when release]" + state()); 
			}
			return;
		}

		if(node.isReset()){//syn(this)
			node.close();
			aliveCount.decrementAndGet();
			aliveTotal.decrementAndGet();
			if (LOG.isDebugEnabled()) {
				LOG.debug("关闭连接[reseted when release]" + state()); 
			}
			return;
		}

		lock.lock();
		try{
			if(freeCount.get() >= core){
				node.close();
				aliveCount.decrementAndGet();
				aliveTotal.decrementAndGet();
				if (LOG.isDebugEnabled()) {
					LOG.debug("关闭连接[free full when release]" + state()); 
				}
				return;
			}

			if(!freeQueue.offer(node)){
				node.close();
				aliveCount.decrementAndGet();
				aliveTotal.decrementAndGet();
				if (LOG.isDebugEnabled()) {
					LOG.debug("关闭连接[release failed]" + state()); 
				}
				return;
			}

			freeCount.incrementAndGet();
			if (LOG.isDebugEnabled()) {
				LOG.debug("释放连接" + state()); 
			}
			condition.signalAll();
		}finally{
			lock.unlock();
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

	/**
	 * 将同步操作的单元由pool改为node,原先的pool实现要实现动态响应配置变化非常麻烦
	 * 修改之后只需要在acquire()和release()时判断node是否 进行isReset()
	 * 
	 * @author X4584
	 * @date 2018年12月13日
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
