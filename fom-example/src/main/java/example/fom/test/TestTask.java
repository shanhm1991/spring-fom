package example.fom.test;

import org.apache.commons.lang3.RandomUtils;
import org.eto.fom.context.core.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 */
public class TestTask extends Task<Long> {
	
	private static final Logger LOG = LoggerFactory.getLogger("test");

	public TestTask(int i) {
		super("TestTask-" + i);
	}

	@Override
	public Long exec() { 
		long sleep = RandomUtils.nextLong(1000, 5000);
		try {
			Thread.sleep(sleep);
		} catch (InterruptedException e) {
			LOG.info("task[{}] cancled due to interrupt.", id);
			return sleep;
		} 
		return sleep;
	}
}
