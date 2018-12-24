package com.fom.context;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
class TimedExecutorPool extends ThreadPoolExecutor {

	public TimedExecutorPool(int minSize, long keepAliveTime, BlockingQueue<Runnable> workQueue) {
		super(minSize, 20, keepAliveTime, TimeUnit.SECONDS, workQueue);
	}

	@Override
	protected <T> TimedFuture<T> newTaskFor(Runnable runnable, T value) {
		return new TimedFuture<T>(runnable, value);
	}

	@Override
	public TimedFuture<Void> submit(Runnable task) {
		if (task == null) 
			throw new NullPointerException(); 
		TimedFuture<Void> future = newTaskFor(task, null);
		execute(future);
		return future;
	}

}
