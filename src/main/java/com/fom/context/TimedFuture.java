package com.fom.context;

import java.util.concurrent.FutureTask;

/**
 * 
 * @author shanhm1991
 *
 * @param <T>
 */
class TimedFuture<T> extends FutureTask<T> {

	private long createTime;

	public TimedFuture(Runnable runnable, T result) {
		super(runnable, result);
		createTime = System.currentTimeMillis() / 1000;
	}

	public long getCreateTime() {
		return createTime;
	}
}
