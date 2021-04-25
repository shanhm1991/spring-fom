package org.springframework.fom.boot.test;

import java.util.Random;

import org.springframework.fom.Task;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class TestTask extends Task<Long> {
	
	private final Random random = new Random();
	
	public TestTask(int i) {
		super("TestTask-" + i);
	}

	@Override
	public Long exec() { 
		long sleep = random.nextInt(5000);
		try {
			logger.info("task executing ...");
			Thread.sleep(sleep);
		} catch (InterruptedException e) {
			logger.info("task cancled due to interrupt.");
			return sleep;
		} 
		return sleep;
	}
}
