package org.springframework.fom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.fom.interceptor.ScheduleCompleter;
import org.springframework.fom.interceptor.ScheduleFactory;
import org.springframework.fom.interceptor.ScheduleTerminator;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ScheduleContext<E> implements ScheduleFactory<E>, ScheduleCompleter<E>, ScheduleTerminator {

	protected final ScheduleConfig ScheduleConfig = new ScheduleConfig();
	
	private String scheduleName;

	private String scheduleBeanName;

	private Logger logger;
	
	public String getScheduleName() {
		return scheduleName;
	}

	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}

	public void setScheduleBeanName(String scheduleBeanName) {
		this.scheduleBeanName = scheduleBeanName;
	}

	public String getScheduleBeanName() {
		return scheduleBeanName;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public ScheduleConfig getScheduleConfig() {
		return ScheduleConfig;
	}

	@Override
	public void onScheduleTerminate(long execTimes, long lastExecTime) {
	}

	@Override
	public void onScheduleComplete(long execTimes, long lastExecTime, List<Result<E>> results) throws Exception {

	}

	@Override
	public Collection<? extends Task<E>> newSchedulTasks() throws Exception {
		return new ArrayList<>();
	}



}
