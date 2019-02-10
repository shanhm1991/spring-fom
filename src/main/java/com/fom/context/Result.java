package com.fom.context;

/**
 * 
 * Executor执行结果
 * 
 * @author shanhm
 *
 */
public class Result {
	
	 final String sourceUri;
	
	 boolean success;
	
	 long startTime;
	
	 long costTime;
	
	 Throwable throwable;
	
	public Result(String sourceUri) {
		this.sourceUri = sourceUri;
	}

	public String getSourceUri() {
		return sourceUri;
	}

	public boolean isSuccess() {
		return success;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getCostTime() {
		return costTime;
	}

	public Throwable getThrowable() {
		return throwable;
	}

}
