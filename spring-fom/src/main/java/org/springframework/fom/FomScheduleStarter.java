package org.springframework.fom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class FomScheduleStarter implements SmartLifecycle, ApplicationContextAware {
	
	private static Logger logger = LoggerFactory.getLogger(FomScheduleStarter.class);

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void start() {
		String[] scheduleNames = applicationContext.getBeanNamesForType(ScheduleContext.class);
		for(String scheduleName : scheduleNames){
			ScheduleContext<?> schedule = (ScheduleContext)applicationContext.getBean(scheduleName);
			logger.info("load schedule[{}]: {}", scheduleName, schedule.getScheduleConfig().getConfMap()); 
			schedule.scheduleStart();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void stop() {
		String[] scheduleNames = applicationContext.getBeanNamesForType(ScheduleContext.class);
		for(String scheduleName : scheduleNames){
			ScheduleContext<?> schedule = (ScheduleContext)applicationContext.getBean(scheduleName);
			schedule.scheduleShutdown();
		}
	}

	@Override
	public boolean isRunning() {
		return false;
	}
}
