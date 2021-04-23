package org.springframework.fom.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.fom.Result;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.Task;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@FomSchedule(
		cron = "0/3 * * * * ?", 
		threadCore=10, 
		threadMax=20, 
		queueSize=500, 
		threadAliveTime=100, 
		taskOverTime=300, 
		execOnLoad=true, 
		remark="备注测试")
public class TestContextBean extends ScheduleContext<Long> {

	@Value("${conf.user}@${conf.address}")
	private String email;
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public void onScheduleComplete(long execTimes, long lastExecTime, List<Result<Long>> results) throws Exception {
		logger.info("schedule complete: execTimes={}, lastExecTime={}, results={}", execTimes, lastExecTime, results);
	}

	@Override
	public void onScheduleTerminate(long execTimes, long lastExecTime) {
		logger.info("schedule terminate: execTimes={}, lastExecTime={}", execTimes, lastExecTime);
	}

	@Override
	public Collection<? extends Task<Long>> newSchedulTasks() throws Exception {
		Task<Long> task = new Task<Long>("testTask"){ 
			@Override
			public Long exec() throws Exception {
				logger.info("task execute...");
				return 20L;
			}
		};

		List<Task<Long>> list = new ArrayList<>();
		list.add(task);
		return list;
	}

	@Scheduled
	public long test(){
		logger.info("task execute...");
		return 20;
	}
}
