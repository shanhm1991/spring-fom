package org.springframework.fom;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ScheduleInfo {

	private final String scheduleName;

	private final String scheduleBeanName;
	
	private final String state;
	
	public ScheduleInfo(ScheduleContext<?> scheduleContext){
		this.scheduleName = scheduleContext.getScheduleName();
		this.scheduleBeanName = scheduleContext.getScheduleBeanName();
		this.state = scheduleContext.getState().name();
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public String getScheduleBeanName() {
		return scheduleBeanName;
	}

	public String getState() {
		return state;
	}
	
}
