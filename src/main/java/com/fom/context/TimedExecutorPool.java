package com.fom.context;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author shanhm
 *
 */
class TimedExecutorPool extends ThreadPoolExecutor { 

	private Map<String, Thread> threadMap = new ConcurrentHashMap<>();

	public TimedExecutorPool(int core, int max, long aliveTime, BlockingQueue<Runnable> workQueue) {
		super(core, max, aliveTime, TimeUnit.SECONDS, workQueue);
	}

	@Override
	protected <T> TimedFuture<T> newTaskFor(Runnable runnable, T value) {
		return new TimedFuture<T>(runnable, value);
	}

	@Override
	protected <T> TimedFuture<T> newTaskFor(Callable<T> callable) {
		return new TimedFuture<T>(callable);
	}

	@SuppressWarnings({"rawtypes" })
	@Override
	protected void beforeExecute(Thread t, Runnable r) { 
		if(r instanceof TimedFuture){
			TimedFuture future = (TimedFuture)r;
			threadMap.put(future.getName(), t);
		}
	}

	@SuppressWarnings({"rawtypes" })
	@Override
	protected void afterExecute(Runnable r, Throwable t) { 
		if(r instanceof TimedFuture){
			TimedFuture future = (TimedFuture)r;
			threadMap.remove(future.getName());
		}
	}

	@Override
	public TimedFuture<Boolean> submit(Runnable runnable) { 
		if (runnable == null) 
			throw new NullPointerException(); 
		TimedFuture<Boolean> future = newTaskFor(runnable, true);
		execute(future);
		return future;
	}

	@Override
	public <T> TimedFuture<T> submit(Callable<T> callable) { 
		TimedFuture<T> future = newTaskFor(callable);
		execute(future);
		return future;
	}

	Collection<Thread> getActiveThreads() {
		return threadMap.values();
	}
}
