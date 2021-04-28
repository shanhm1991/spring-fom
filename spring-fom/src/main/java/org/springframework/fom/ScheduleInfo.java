package org.springframework.fom;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ScheduleInfo {
	
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private final String scheduleName;

	private final String scheduleBeanName;
	
	private final String state;
	
	private final String stateTitile;
	
	private final String stateImage;
	
	private final long scheduleTimes;
	
	private final String loadTime;
	
	private final String lastTime;
	
	private final String nextTime;
	
	private final long success;
	
	private final long failed;
	
	private final int waiting;
	
	private final long active;
	
	private final Map<String, Object> config;
	
	private final String loggerName;
	
	private String loggerLevel;
	
	public ScheduleInfo(ScheduleContext<?> scheduleContext){
		this.scheduleName = scheduleContext.getScheduleName();
		this.scheduleBeanName = scheduleContext.getScheduleBeanName();
		this.scheduleTimes = scheduleContext.getSchedulTimes();
		this.loadTime = dateFormat.format(scheduleContext.getLoadTime());
		this.lastTime = dateFormat.format(scheduleContext.getLastTime());
		this.nextTime = dateFormat.format(scheduleContext.getNextTime());
		this.loggerName = scheduleContext.getLogger().getName();
		
		State state = scheduleContext.getState();
		this.state = state.name();
		this.stateTitile = state.title();
		this.stateImage = state.src();
		
		ScheduleStatistics scheduleStatistics = scheduleContext.getScheduleStatistics();
		this.success = scheduleStatistics.getSuccess();
		this.failed = scheduleStatistics.getFailed();
		
		ScheduleConfig scheduleConfig = scheduleContext.getScheduleConfig();
		this.waiting = scheduleConfig.getWaitings();
		this.active = scheduleConfig.getActives();
		
		this.config = new HashMap<>();
		config.putAll(scheduleConfig.getConfMap());
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
	
	public String getLoadTime() {
		return loadTime;
	}

	public String getLoggerLevel() {
		return loggerLevel;
	}

	public void setLoggerLevel(String loggerLevel) {
		this.loggerLevel = loggerLevel;
	}

	public long getScheduleTimes() {
		return scheduleTimes;
	}

	public String getLastTime() {
		return lastTime;
	}

	public String getNextTime() {
		return nextTime;
	}

	public String getLoggerName() {
		return loggerName;
	}

	public long getSuccess() {
		return success;
	}

	public long getFailed() {
		return failed;
	}

	public long getWaiting() {
		return waiting;
	}

	public long getActive() {
		return active;
	}
	
	public String getStateTitile() {
		return stateTitile;
	}

	public String getStateImage() {
		return stateImage;
	}
	
	public Map<String, Object> getConfig() {
		return config;
	}

	@Override
	public String toString() {
		return "{scheduleName=" + scheduleName + ", scheduleBeanName=" + scheduleBeanName + ", state="
				+ state + ", scheduleTimes=" + scheduleTimes + ", loadTime=" + loadTime + ", lastTime=" + lastTime
				+ ", nextTime=" + nextTime + ", success=" + success + ", failed=" + failed + ", waiting=" + waiting
				+ ", active=" + active + ", loggerName=" + loggerName + ", loggerLevel=" + loggerLevel + "}";
	}
}
