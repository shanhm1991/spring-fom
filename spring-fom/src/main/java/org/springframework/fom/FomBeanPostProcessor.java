package org.springframework.fom;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

//import org.apache.commons.lang3.StringUtils;
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
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class FomBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, EmbeddedValueResolverAware {

	private BeanFactory beanFactory;

	private StringValueResolver valueResolver;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver stringValueResolver) {
		this.valueResolver = stringValueResolver;
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
		if(!StringUtils.isEmpty(scheduleBeanName)){ 
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
		scheduleConfig.reset();

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
		if(!StringUtils.isEmpty(cron)){
			cron = valueResolver.resolveStringValue(cron);
			scheduleConfig.setCron(cron); 
			return true;
		}
		return false;
	}

	private boolean setFixedRate(ScheduleConfig scheduleConfig, long fixedRate, String fixedRateString){
		if(!StringUtils.isEmpty(fixedRateString)){
			fixedRateString = valueResolver.resolveStringValue(fixedRateString);
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
		if(!StringUtils.isEmpty(fixedDelayString)){
			fixedDelayString = valueResolver.resolveStringValue(fixedDelayString);
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
		if(!StringUtils.isEmpty(threadCoreString)){
			threadCoreString = valueResolver.resolveStringValue(threadCoreString);
			int threadCore = Integer.parseInt(threadCoreString);
			if(ScheduleConfig.THREAD_MIN != threadCore){
				scheduleConfig.setThreadCore(threadCore);
			}
		}else if(ScheduleConfig.THREAD_MIN != fomSchedule.threadCore()){
			scheduleConfig.setThreadCore(fomSchedule.threadCore());
		}

		String threadMaxString = fomSchedule.threadMaxString();
		if(!StringUtils.isEmpty(threadMaxString)){
			threadMaxString = valueResolver.resolveStringValue(threadMaxString);
			int threadMax = Integer.parseInt(threadMaxString);
			if(ScheduleConfig.THREAD_MIN != threadMax){
				scheduleConfig.setThreadMax(threadMax);
			}
		}else if(ScheduleConfig.THREAD_MIN != fomSchedule.threadMax()){
			scheduleConfig.setThreadMax(fomSchedule.threadMax());
		}

		String queueSizeString = fomSchedule.queueSizeString();
		if(StringUtils.isEmpty(queueSizeString) 
				|| !scheduleConfig.setQueueSize(Integer.parseInt(valueResolver.resolveStringValue(queueSizeString)))){
			scheduleConfig.setQueueSize(fomSchedule.queueSize());
		}

		String threadAliveTimeString = fomSchedule.threadAliveTimeString(); 
		if(StringUtils.isEmpty(threadAliveTimeString) 
				|| !scheduleConfig.setThreadAliveTime(Integer.parseInt(valueResolver.resolveStringValue(threadAliveTimeString)))){
			scheduleConfig.setThreadAliveTime(fomSchedule.threadAliveTime());
		}

		scheduleConfig.setRemark(valueResolver.resolveStringValue(fomSchedule.remark()));

		String execOnLoadString = fomSchedule.execOnLoadString();
		if(StringUtils.isEmpty(execOnLoadString) 
				|| !scheduleConfig.setExecOnLoad(Boolean.parseBoolean(valueResolver.resolveStringValue(execOnLoadString)))){
			scheduleConfig.setExecOnLoad(fomSchedule.execOnLoad()); 
		}

		String taskOverTimeString = fomSchedule.taskOverTimeString();
		if(StringUtils.isEmpty(taskOverTimeString) 
				|| !scheduleConfig.setTaskOverTime(Integer.parseInt(valueResolver.resolveStringValue(taskOverTimeString)))){
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
					String confValue = valueResolver.resolveStringValue(ex);
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
