package org.springframework.fom.support.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.ScheduleInfo;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Validated
public class FomServiceImpl implements FomService, ApplicationContextAware{

	private ApplicationContext applicationContext;

	private final Map<String, ScheduleContext<?>> scheduleMap = new HashMap<>();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@PostConstruct 
	private void init(){
		String[] scheduleNames = applicationContext.getBeanNamesForType(ScheduleContext.class);
		for(String scheduleName : scheduleNames){
			scheduleMap.put(scheduleName, (ScheduleContext<?>)applicationContext.getBean(scheduleName));
		}
	}

	@Override
	public List<ScheduleInfo> list() {
		List<ScheduleInfo> list = new ArrayList<>(scheduleMap.size());
		for(ScheduleContext<?> schedule : scheduleMap.values()){
			list.add(schedule.getScheduleInfo());
		}
		return list;
	}

	@Override
	public ScheduleInfo info(String scheduleName) {
		ScheduleContext<?> schedule = scheduleMap.get(scheduleName); 
		Assert.notNull(schedule, "schedule names " + scheduleName + " not exist.");
		return schedule.getScheduleInfo();
	}

	@Override
	public ScheduleInfo info(Class<?> clazz) {
		if(ScheduleContext.class.isAssignableFrom(clazz)){ 
			ScheduleContext<?> scheduleContext = (ScheduleContext<?>)applicationContext.getBean(clazz);
			Assert.notNull(scheduleContext, "schedule of " + clazz + " not exist.");
			return scheduleContext.getScheduleInfo();
		}

		String[] beanNames = applicationContext.getBeanNamesForType(clazz);
		Assert.isTrue(beanNames != null && beanNames.length == 1, "cannot determine schedule by class:" + clazz); 

		String beanName = beanNames[0];
		ScheduleContext<?> scheduleContext = (ScheduleContext<?>)applicationContext.getBean("$" + beanName);
		Assert.notNull(scheduleContext, "schedule of " + clazz + " not exist.");

		return scheduleContext.getScheduleInfo();
	}
}
