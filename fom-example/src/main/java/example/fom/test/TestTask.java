package example.fom.test;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.RandomUtils;
import org.eto.fom.context.core.Task;

/**
 * 
 * @author shanhm
 *
 */
public class TestTask extends Task<Long> {

	public TestTask(int i) {
		super("TestTask-" + i);
	}

	@Override
	public Long exec() { 
		String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
		long sleep = RandomUtils.nextLong(1000, 5000);
		try {
			Thread.sleep(sleep);
		} catch (InterruptedException e) {
			System.out.println(now + " task[" + id + "] cancled due to interrupt.");
			return sleep;
		} 
		System.out.println(now + " task[" + id + "] finished, and cost time " + sleep);
		return sleep;
	}
}
