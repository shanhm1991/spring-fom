package org.springframework.fom;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
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
	public void setEmbeddedValueResolver(StringValueResolver stringValueResolver) {
		this.stringValueResolver = stringValueResolver;
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

		Logger logger;
		FomSchedule fomSchedule = clazz.getAnnotation(FomSchedule.class);
		if(fomSchedule == null){ // 如果scheduleBean本身就是一个代理类，那么就获取不到FomSchedule.class
			fomSchedule = scheduleBean.getClass().getAnnotation(FomSchedule.class);
			logger = LoggerFactory.getLogger(scheduleBean.getClass());
		}else{
			logger = LoggerFactory.getLogger(clazz);
		}

		scheduleContext.setLogger(logger); 
		ScheduleConfig scheduleConfig = scheduleContext.getScheduleConfig();
		if(fomSchedule != null){ // 注解引入
			setCronConf(scheduleConfig, fomSchedule, scheduleContext, scheduleBean);
			setOtherConf(scheduleConfig, fomSchedule);
			setValue(scheduleConfig, scheduleContext, scheduleBean);
			
		}else{ // xml配置

		}
		scheduleConfig.init();

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(new ScheduleProxy(beanName, scheduleContext, fomSchedule, scheduleBean));
		Object obj = enhancer.create();
		return obj;
	}

	// 只要获取到一个就结束
	private void setCronConf(ScheduleConfig scheduleConfig, FomSchedule fomSchedule, ScheduleContext<?> scheduleContext, Object scheduleBean){
		if(fomSchedule != null){
			if(setCron(scheduleConfig, fomSchedule.cron())
					|| setFixedRate(scheduleConfig, fomSchedule.fixedRate(), fomSchedule.fixedRateString())
					|| setFixedDelay(scheduleConfig, fomSchedule.fixedDelay(), fomSchedule.fixedDelayString())){ 
				return;
			}
		}
		// 尝试从方法上获取定时计划
		if(scheduleBean != null){ // 从scheduleBean中获取
			Class<?> clazz = scheduleBean.getClass();
			for(Method method : clazz.getMethods()){
				Scheduled scheduled = method.getAnnotation(Scheduled.class);
				if(scheduled != null){
					if(setCron(scheduleConfig, scheduled.cron())
							|| setFixedRate(scheduleConfig, scheduled.fixedRate(), scheduled.fixedRateString())
							|| setFixedDelay(scheduleConfig, scheduled.fixedDelay(), scheduled.fixedDelayString())){ 
						return;
					}
				}
			}
		}else{ // 从自身获取
			Class<?> clazz = scheduleContext.getClass();
			for(Method method : clazz.getMethods()){
				Scheduled scheduled = method.getAnnotation(Scheduled.class);
				if(scheduled != null){
					if(setCron(scheduleConfig, scheduled.cron())
							|| setFixedRate(scheduleConfig, scheduled.fixedRate(), scheduled.fixedRateString())
							|| setFixedDelay(scheduleConfig, scheduled.fixedDelay(), scheduled.fixedDelayString())){ 
						return;
					}
				}
			}
		}
	}

	private boolean setCron(ScheduleConfig scheduleConfig, String cron){
		if(StringUtils.isNotBlank(cron)){
			cron = stringValueResolver.resolveStringValue(cron);
			scheduleConfig.setCron(cron); 
			return true;
		}
		return false;
	}

	private boolean setFixedRate(ScheduleConfig scheduleConfig, long fixedRate, String fixedRateString){
		if(StringUtils.isNotBlank(fixedRateString)){
			fixedRateString = stringValueResolver.resolveStringValue(fixedRateString);
			if(scheduleConfig.setFixedRate(Long.parseLong(fixedRateString))){
				return true;
			}
		}
		if(scheduleConfig.setFixedRate(fixedRate)){
			return true;
		}
		return false;
	}

	private boolean setFixedDelay(ScheduleConfig scheduleConfig, long fixedDelay, String fixedDelayString){
		if(StringUtils.isNotBlank(fixedDelayString)){
			fixedDelayString = stringValueResolver.resolveStringValue(fixedDelayString);
			if(scheduleConfig.setFixedDelay(Long.parseLong(fixedDelayString))){
				return true;
			}
		}
		if(scheduleConfig.setFixedDelay(fixedDelay)){
			return true;
		}
		return false;
	}

	private void setOtherConf(ScheduleConfig scheduleConfig, FomSchedule fomSchedule){
		String threadCoreString = fomSchedule.threadCoreString();
		if(StringUtils.isBlank(threadCoreString) 
				|| !scheduleConfig.setThreadCore(Integer.parseInt(threadCoreString))){
			scheduleConfig.setThreadCore(fomSchedule.threadCore());
		}

		String threadMaxString = fomSchedule.threadMaxString();
		if(StringUtils.isBlank(threadMaxString) 
				|| !scheduleConfig.setThreadMax(Integer.parseInt(threadMaxString))){
			scheduleConfig.setThreadMax(fomSchedule.threadMax());
		}

		String queueSizeString = fomSchedule.queueSizeString();
		if(StringUtils.isBlank(queueSizeString) 
				|| !scheduleConfig.setQueueSize(Integer.parseInt(queueSizeString))){
			scheduleConfig.setQueueSize(fomSchedule.queueSize());
		}

		String threadAliveTimeString = fomSchedule.threadAliveTimeString(); 
		if(StringUtils.isBlank(threadAliveTimeString) 
				|| !scheduleConfig.setThreadAliveTime(Integer.parseInt(threadAliveTimeString))){
			scheduleConfig.setThreadAliveTime(fomSchedule.threadAliveTime());
		}

		scheduleConfig.setRemark(fomSchedule.remark());

		String execOnLoadString = fomSchedule.execOnLoadString();
		if(StringUtils.isBlank(execOnLoadString) 
				|| !scheduleConfig.setExecOnLoad(Boolean.parseBoolean(execOnLoadString))){
			scheduleConfig.setExecOnLoad(fomSchedule.execOnLoad()); 
		}

		String taskOverTimeString = fomSchedule.taskOverTimeString();
		if(StringUtils.isBlank(taskOverTimeString) 
				|| !scheduleConfig.setTaskOverTime(Integer.parseInt(taskOverTimeString))){
			scheduleConfig.setTaskOverTime(fomSchedule.taskOverTime());
		}
	}

	private void setValue(ScheduleConfig scheduleConfig, ScheduleContext<?> scheduleContext, Object scheduleBean){
		Field[] fields = null;
		if(scheduleBean != null){
			fields = scheduleBean.getClass().getDeclaredFields();
		}else{
			fields = scheduleContext.getClass().getDeclaredFields();
		}

		for(Field field : fields){
			Value value = field.getAnnotation(Value.class);
			if(value != null){
				List<String> list = getProperties(value.value());
				for(String ex : list){
					String confValue = stringValueResolver.resolveStringValue(ex);
					int index = ex.indexOf(":");
					if(index == -1){
						index = ex.indexOf("}");
					}
					String key = ex.substring(2, index);
					scheduleConfig.set(key, confValue);
				}
			}
		}
	}

	private List<String> getProperties(String expression){
		List<String> list = new ArrayList<>();
		int begin;
		int end = 0;
		while((begin = expression.indexOf("${", end)) != -1){
			end = expression.indexOf("}", begin);
			list.add(expression.substring(begin, end + 1));
		}
		return list;
	}

}
