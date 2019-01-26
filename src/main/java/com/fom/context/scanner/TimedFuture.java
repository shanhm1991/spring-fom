package com.fom.context.scanner;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 
 * @author shanhm
 *
 * @param <T>
 */
public class TimedFuture<T> extends FutureTask<T> {

	private long createTime;

	public TimedFuture(Runnable runnable, T result) {
		super(runnable, result);
		createTime = System.currentTimeMillis();
	}
	
	public TimedFuture(Callable<T> callable){
		super(callable);
		createTime = System.currentTimeMillis();
	}

	public long getCreateTime() {
		return createTime;
	}
}
