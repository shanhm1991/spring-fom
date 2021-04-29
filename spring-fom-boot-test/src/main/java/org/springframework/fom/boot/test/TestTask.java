package org.springframework.fom.boot.test;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.fom.Task;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class TestTask extends Task<Long> {
	
	public TestTask(int i) {
		super("TestTask-" + i);
	}

	@Override
	public Long exec() { 
		long sleep = RandomUtils.nextLong(3000, 6000);
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
