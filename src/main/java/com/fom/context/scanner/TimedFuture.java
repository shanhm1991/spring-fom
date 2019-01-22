package com.fom.context.scanner;

import java.util.concurrent.FutureTask;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <T>
 */
public class TimedFuture<T> extends FutureTask<T> {

	private long createTime;

	public TimedFuture(Runnable runnable, T result) {
		super(runnable, result);
		createTime = System.currentTimeMillis() / 1000;
	}

	public long getCreateTime() {
		return createTime;
	}
}
