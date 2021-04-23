package org.springframework.fom.boot.schedules;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@FomSchedule(remark = "定时单任务测试")
public class SingleScheduleTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(SingleScheduleTest.class);
	
	@Scheduled(fixedDelay = 7)
	public long test(){
		long sleep = RandomUtils.nextLong(1000, 5000);
		try {
			LOG.info("task executing ...");
			Thread.sleep(sleep);
		} catch (InterruptedException e) {
			LOG.info("task cancled due to interrupt.");
			return sleep;
		} 
		return sleep;
	}
}
