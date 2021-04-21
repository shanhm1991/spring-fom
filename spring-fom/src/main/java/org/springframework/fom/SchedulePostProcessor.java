package org.springframework.fom;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.fom.interceptor.ScheduleProxy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Component
public class SchedulePostProcessor implements BeanPostProcessor, BeanFactoryAware, EmbeddedValueResolverAware {

	private BeanFactory beanFactory;
	
	private StringValueResolver stringValueResolver;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
	
	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		
	}

	// handleTimeOut TODO

	@SuppressWarnings("rawtypes")
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> clazz = bean.getClass();
		if(!(ScheduleContext.class.isAssignableFrom(clazz))){
			return bean;
		}

		ScheduleContext scheduleContext = (ScheduleContext)bean;

		String scheduleBeanName = scheduleContext.getScheduleBeanName();
		Object scheduleBean = null;
		if(StringUtils.isNoneBlank(scheduleBeanName)){ 
			scheduleBean = beanFactory.getBean(scheduleBeanName);
		}

		FomSchedule fomSchedule = clazz.getAnnotation(FomSchedule.class);
		if(fomSchedule == null){ // 如果scheduleBean本身就是一个代理类，那么就获取不到FomSchedule.class
			fomSchedule = scheduleBean.getClass().getAnnotation(FomSchedule.class);
		}

		ScheduleConfig scheduleConfig = scheduleContext.getScheduleConfig();
		if(fomSchedule != null){ // 注解引入
			setCronValue(scheduleContext, fomSchedule, scheduleBean, scheduleConfig);
			setOtherValue(fomSchedule, scheduleConfig);
		}else{ // xml配置
			
		}
		scheduleConfig.init();

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(new ScheduleProxy(beanName, scheduleContext, fomSchedule, scheduleBean));
		return enhancer.create();
	}

	private void setCronValue(ScheduleContext<?> scheduleContext, FomSchedule fomSchedule, Object scheduleBean, ScheduleConfig scheduleConfig){
		String cron = null;
		long fixedRate = 0;
		long fixedDelay = 0;
		if(fomSchedule != null){
			cron = fomSchedule.cron();
			fixedRate = fomSchedule.fixedRate();
			fixedDelay = fomSchedule.fixedDelay();
		}
		
		// 尝试从方法上获取定时计划
		if(cron == null && fixedRate == 0 && fixedDelay == 0){
			if(scheduleBean != null){ 
				// 从scheduleBean中获取
				Class<?> clazz = scheduleBean.getClass();
				for(Method method : clazz.getMethods()){
					Scheduled scheduled = method.getAnnotation(Scheduled.class);
					if(scheduled == null){
						continue;
					}
					if(StringUtils.isBlank(cron)){
						cron = scheduled.cron();
					}
					if(fixedRate <= 0){
						fixedRate = scheduled.fixedRate();
					}
					if(fixedDelay <= 0){
						fixedDelay = scheduled.fixedDelay();
					}
				}
			}else{
				// 从自己身上获取
				Class<?> clazz = scheduleContext.getClass();
				for(Method method : clazz.getMethods()){
					Scheduled scheduled = method.getAnnotation(Scheduled.class);
					if(scheduled == null){
						continue;
					}
					if(StringUtils.isBlank(cron)){
						cron = scheduled.cron();
					}
					if(fixedRate <= 0){
						fixedRate = scheduled.fixedRate();
					}
					if(fixedDelay <= 0){
						fixedDelay = scheduled.fixedDelay();
					}
				}
			}
		}
		
		if(StringUtils.isNoneBlank(cron)){
			cron = stringValueResolver.resolveStringValue(cron);
			scheduleConfig.setCron(cron);
		}
		scheduleConfig.setFixedDelay(fixedDelay);
		scheduleConfig.setFixedRate(fixedRate);
	}
	
	private void setOtherValue(FomSchedule fomSchedule, ScheduleConfig scheduleConfig){
		scheduleConfig.setThreadCore(fomSchedule.threadCore());
		scheduleConfig.setThreadMax(fomSchedule.threadMax());
		scheduleConfig.setQueueSize(fomSchedule.queueSize());
		scheduleConfig.setThreadAliveTime(fomSchedule.threadAliveTime());
		 
		scheduleConfig.setRemark(fomSchedule.remark());
		scheduleConfig.setExecOnLoad(fomSchedule.execOnLoad()); 
		scheduleConfig.setTaskOverTime(fomSchedule.taskOverTime());
	}
}
