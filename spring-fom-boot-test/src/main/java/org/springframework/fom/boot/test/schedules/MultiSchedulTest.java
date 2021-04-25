package org.springframework.fom.boot.test.schedules;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.fom.Result;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.fom.interceptor.ScheduleCompleter;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@FomSchedule(remark = "定时多任务测试", cron = "0 0/5 * * * ?", threadCore = 2, taskOverTime = 4)
public class MultiSchedulTest implements ScheduleCompleter<Object> {
	
	private static final Logger LOG = LoggerFactory.getLogger(MultiSchedulTest.class);
	
	private final Random random = new Random();

	@Scheduled
	public long test1(){
		try {
			LOG.info("task executing ...");
			Thread.sleep(random.nextInt(5000));
		} catch (InterruptedException e) {
			LOG.info("task cancled due to interrupt.");
		} 
		return System.currentTimeMillis();
	}
	
	@Scheduled
	public String test2(){
		try {
			LOG.info("task executing ...");
			Thread.sleep(random.nextInt(5000));
		} catch (InterruptedException e) {
			LOG.info("task cancled due to interrupt.");
		} 
		return "test2";
	}

	@Override
	public void onScheduleComplete(long schedulTimes, long schedulTime, List<Result<Object>> results) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(schedulTime);
		LOG.info( "第{}次在{}提交的任务全部完成，结果为{}", date, date, results);
	}
}
