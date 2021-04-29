package org.springframework.fom.boot.test.schedules;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
	
	private final Random random = new Random();
	
	@Value("${conf.user:shanhm1991}@${conf.address:163.com}")
	private String email;
	
	@Scheduled(fixedRate = 30)
	public long test(){
		long sleep = random.nextInt(5000);
		try {
			LOG.info("task executing ..., email={}", email);
			Thread.sleep(sleep);
		} catch (InterruptedException e) {
			LOG.info("task cancled due to interrupt.");
			return sleep;
		} 
		return sleep;
	}
}
