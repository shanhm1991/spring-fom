package org.eto.fom.context.core;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 
 * @author shanhm
 *
 * @param <T> 结果类型
 */
public class TimedFuture<T> extends FutureTask<T> {
	
	private Task<?> task;

	public TimedFuture(Runnable runnable, T result) {
		super(runnable, result);
	}

	public TimedFuture(Callable<T> callable){
		super(callable);
		if(callable instanceof Task){
			 task = ((Task<?>)callable);
		}
	}

	public long getCreateTime() {
		return task.getCreateTime();
	}
	
	public long getStartTime() {
		return task.getStartTime();
	}

	public String getContextName(){
		return task.getName();
	}

	public String getTaskId() {
		return task.id;
	}

	public Task<?> getTask(){
		return task;
	}
}
