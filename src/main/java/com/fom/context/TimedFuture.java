package com.fom.context;

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
	
	private Executor executor;
	
	public TimedFuture(Runnable runnable, T result) {
		super(runnable, result);
		createTime = System.currentTimeMillis();
	}
	
	public TimedFuture(Callable<T> callable){
		super(callable);
		createTime = System.currentTimeMillis();
		if(callable instanceof Executor){
			executor = (Executor)callable;
		}
	}

	public long getCreateTime() {
		return createTime;
	}
	
	public Executor getExecutor(){
		return executor;
	}
}
