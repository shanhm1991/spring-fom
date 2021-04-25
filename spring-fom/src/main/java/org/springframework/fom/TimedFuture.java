package org.springframework.fom;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 
 * @author shanhm1991@163.com
 *
 * @param <T> 结果类型
 */
class TimedFuture<T> extends FutureTask<T> {
	
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

	public String getScheduleName(){
		return task.getScheduleName();
	}

	public String getTaskId() {
		return task.getTaskId(); 
	}

	public Task<?> getTask(){
		return task;
	}
}
