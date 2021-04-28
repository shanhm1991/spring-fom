package org.springframework.fom.boot.test.schedules;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.fom.Result;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.fom.boot.test.TestTask;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@FomSchedule(fixedRate = 15, threadCore = 4)
public class BatchFomContextTest extends ScheduleContext<Long> {
	
	@Value("${conf.user:shanhm1991}@${conf.address:163.com}")
	private String email;

	@Override
	public List<TestTask> newSchedulTasks() throws Exception {
		List<TestTask> tasks = new ArrayList<>();
		for(int i = 0;i < 5; i++){
			tasks.add(new TestTask(i));
		} 
		return tasks;
	} 
	
	@Override
	public void onScheduleComplete(long batchTimes, long batchTime, List<Result<Long>> results) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(batchTime);
		logger.info("第{}次在{}提交的任务全部完成, 当前email={}", batchTimes, date, email);
	}
}
