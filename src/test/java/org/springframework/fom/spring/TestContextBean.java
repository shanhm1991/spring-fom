package org.springframework.fom.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.fom.Result;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.Task;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.scheduling.annotation.Scheduled;

@FomSchedule(cron = "0/15 * * * * ?", threadCore = 10, threadMax = 20, remark="备注测试")
public class TestContextBean extends ScheduleContext<Long> {
	
	@Value("${conf.user}@${conf.address}")
	private String email;
	
	@Override
	public void onScheduleComplete(long execTimes, long lastExecTime, List<Result<Long>> results) throws Exception {
		Assert.assertEquals(execTimes, 100); 
		Assert.assertEquals(lastExecTime, 100); 
	}

	@Override
	public void onScheduleTerminate(long execTimes, long lastExecTime) {
		Assert.assertEquals(execTimes, 200); 
		Assert.assertEquals(lastExecTime, 200); 
	}
	
	@Override
	public Collection<? extends Task<Long>> newSchedulTasks() throws Exception {
		Task<Long> task = new Task<Long>("testTask"){ 
			@Override
			public Long exec() throws Exception {
				return 20L;
			}
		};
		
		List<Task<Long>> list = new ArrayList<>();
		list.add(task);
		return list;
	}
	
	@Scheduled
	public long test(){
		return 20;
	}
}
