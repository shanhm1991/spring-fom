package org.springframework.fom.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.fom.Result;
import org.springframework.fom.Task;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.fom.interceptor.ScheduleCompleter;
import org.springframework.fom.interceptor.ScheduleFactory;
import org.springframework.fom.interceptor.ScheduleTerminator;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@FomSchedule(
		threadCoreString="${testScheduleBean.threadCore}",
		threadMaxString="${testScheduleBean.threadMax}",
		queueSizeString="${testScheduleBean.queueSize}",
		threadAliveTimeString="${testScheduleBean.threadAliveTime}",
		taskOverTimeString="${testScheduleBean.taskOverTime}",
		execOnLoadString="${testScheduleBean.execOnLoad}",
		remark="${testScheduleBean.remark}")
public class TestScheduleBean implements ScheduleCompleter<Long>, ScheduleFactory<Long>, ScheduleTerminator{ 
	
	private static Logger logger = LoggerFactory.getLogger(TestScheduleBean.class);

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
	public Collection<Task<Long>> newSchedulTasks() throws Exception {
		List<Task<Long>> list = new ArrayList<>();
		list.add(new Task<Long>("test"){
			@Override
			public Long exec() throws Exception {
				logger.info("task execute...");
				return 10L;
			}
		});
		return list;
	}


	@Scheduled(fixedRateString="${testScheduleBean.fixedRate}")
	public long test(){
		logger.info("task execute...");
		return 10;
	}
}
