package com.fom.context;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author shanhm
 *
 */
public class TimedExecutorPool extends ThreadPoolExecutor { 

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
}
