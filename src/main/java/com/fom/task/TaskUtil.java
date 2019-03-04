package com.fom.task;

/**
 * 
 * @author shanhm
 *
 */
class TaskUtil {

	public static void checkInterrupt() throws InterruptedException{
		if(Thread.interrupted()){
			throw new InterruptedException("interrupted when batchProcessLineData");
		}
	}
}
