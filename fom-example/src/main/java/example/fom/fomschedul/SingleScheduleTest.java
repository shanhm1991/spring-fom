package example.fom.fomschedul;

import org.apache.commons.lang3.RandomUtils;
import org.eto.fom.context.annotation.FomSchedul;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(remark = "定时单任务测试")
public class SingleScheduleTest {
	
	private static final Logger LOG = LoggerFactory.getLogger("SingleScheduleTest");
	
	@Scheduled(fixedDelay = 7)
	public long test(){
		long sleep = RandomUtils.nextLong(1000, 5000);
		try {
			Thread.sleep(sleep);
		} catch (InterruptedException e) {
			LOG.info("task cancled due to interrupt.");
			return sleep;
		} 
		return sleep;
	}
}
