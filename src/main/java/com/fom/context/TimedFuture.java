package com.fom.context;

import java.util.concurrent.FutureTask;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
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
