package com.fom.context.scanner;

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

	public TimedExecutorPool(int minSize, long keepAliveTime, BlockingQueue<Runnable> workQueue) {
		super(minSize, 20, keepAliveTime, TimeUnit.SECONDS, workQueue);
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
	public TimedFuture<?> submit(Runnable runnable) { 
		if (runnable == null) 
			throw new NullPointerException(); 
		TimedFuture<Void> future = newTaskFor(runnable, null);
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
