package org.springframework.fom.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.fom.Result;
import org.springframework.fom.Task;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.fom.interceptor.ScheduleCompleter;
import org.springframework.fom.interceptor.ScheduleFactory;
import org.springframework.fom.interceptor.ScheduleTerminator;
import org.springframework.scheduling.annotation.Scheduled;

@FomSchedule
public class TestScheduleBean implements ScheduleCompleter<Long>, ScheduleFactory<Long>, ScheduleTerminator{ 
	
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
	public Collection<Task<Long>> newSchedulTasks() throws Exception {
		List<Task<Long>> list = new ArrayList<>();
		list.add(new Task<Long>("test"){
			@Override
			public Long exec() throws Exception {
				return 10L;
			}
		});
		return list;
	}

	
	@Scheduled
	public long test(){
		return 10;
	}
}
