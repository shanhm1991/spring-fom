package org.springframework.fom;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.ApplicationContextException;
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

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> clazz = bean.getClass();
		if(!(ScheduleContext.class.isAssignableFrom(clazz))){
			return bean;
		}

		ScheduleContext<?> scheduleContext = (ScheduleContext<?>)bean;
		String scheduleBeanName = scheduleContext.getScheduleBeanName(); 
		FomSchedule fomSchedule = scheduleContext.getClass().getAnnotation(FomSchedule.class);
		if(StringUtils.isEmpty(scheduleBeanName) && fomSchedule == null){ // 通过@Bean注入的不需要处理
			scheduleContext.setScheduleName(beanName);  
			scheduleContext.setLogger(LoggerFactory.getLogger(scheduleContext.getClass()));
			return bean;
		}

		Object scheduleBean = null;
		if(!StringUtils.isEmpty(scheduleBeanName)){ 
			scheduleBean = beanFactory.getBean(scheduleBeanName);
		}

		// 获取FomSchedule，顺便设置下Logger
		fomSchedule = clazz.getAnnotation(FomSchedule.class);
		if(fomSchedule == null){ 
			fomSchedule = scheduleBean.getClass().getAnnotation(FomSchedule.class);
			scheduleContext.setLogger(LoggerFactory.getLogger(scheduleBean.getClass())); 
		}else{ 
			scheduleContext.setLogger(LoggerFactory.getLogger(clazz));
		}

		ScheduleConfig scheduleConfig = scheduleContext.getScheduleConfig();
		if(fomSchedule != null){ // 注解引入
			setCronConf(scheduleConfig, fomSchedule, scheduleContext, scheduleBean);
			setOtherConf(scheduleConfig, fomSchedule);
			setValue(scheduleConfig, scheduleContext, scheduleBean);

		}else{ // xml配置方式 TODO

		}

		try {
			loadCache(beanName, scheduleContext);
		} catch (Exception e) {
			throw new ApplicationContextException("", e);
		}
		scheduleConfig.reset();

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(new ScheduleProxy(beanName, scheduleContext, fomSchedule, scheduleBean));
		Object obj = enhancer.create();
		return obj;
	}

	@SuppressWarnings("unchecked")
	private void loadCache(String beanName, ScheduleContext<?> scheduleContext) throws Exception{ 
		if(!Boolean.valueOf(valueResolver.resolveStringValue("${spring.fom.config.cache.enable:true}"))){
			return;
		}

		String configCachePath = valueResolver.resolveStringValue("${spring.fom.config.cache.path:cache/schedule}");
		File file = new File(configCachePath + File.separator + beanName + ".cache");
		if(!file.exists()){
			return;
		}

		try(FileInputStream input = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(input);){
			HashMap<String, Object> map = (HashMap<String, Object>)ois.readObject();

			String cron = (String)map.remove(FomSchedule.CRON);
			ScheduleConfig scheduleConfig = scheduleContext.getScheduleConfig();
			scheduleConfig.setCron(cron);

			Map<String, Object> originalMap = scheduleConfig.getOriginalMap();
			originalMap.putAll(map);

			Collection<List<Field>> fileds = scheduleConfig.getEnvirment().values();
			Set<Field> envirmentField = new HashSet<>();
			for(List<Field> list : fileds){
				envirmentField.addAll(list);
			}
			scheduleContext.valueEnvirmentField(envirmentField);
		}
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
			if(FomSchedule.THREAD_CORE_DEFAULT != threadCore){
				scheduleConfig.setThreadCore(threadCore);
			}
		}else if(FomSchedule.THREAD_CORE_DEFAULT != fomSchedule.threadCore()){
			scheduleConfig.setThreadCore(fomSchedule.threadCore());
		}

		String threadMaxString = fomSchedule.threadMaxString();
		if(!StringUtils.isEmpty(threadMaxString)){
			threadMaxString = valueResolver.resolveStringValue(threadMaxString);
			int threadMax = Integer.parseInt(threadMaxString);
			if(FomSchedule.THREAD_CORE_DEFAULT != threadMax){
				scheduleConfig.setThreadMax(threadMax);
			}
		}else if(FomSchedule.THREAD_CORE_DEFAULT != fomSchedule.threadMax()){
			scheduleConfig.setThreadMax(fomSchedule.threadMax());
		}

		String queueSize = fomSchedule.queueSizeString();
		if(StringUtils.isEmpty(queueSize) 
				|| !scheduleConfig.setQueueSize(Integer.parseInt(valueResolver.resolveStringValue(queueSize)))){
			scheduleConfig.setQueueSize(fomSchedule.queueSize());
		}

		String threadAliveTime = fomSchedule.threadAliveTimeString(); 
		if(StringUtils.isEmpty(threadAliveTime) 
				|| !scheduleConfig.setThreadAliveTime(Integer.parseInt(valueResolver.resolveStringValue(threadAliveTime)))){
			scheduleConfig.setThreadAliveTime(fomSchedule.threadAliveTime());
		}

		scheduleConfig.setRemark(valueResolver.resolveStringValue(fomSchedule.remark()));

		String execOnLoad = fomSchedule.execOnLoadString();
		if(StringUtils.isEmpty(execOnLoad) 
				|| !scheduleConfig.setExecOnLoad(Boolean.parseBoolean(valueResolver.resolveStringValue(execOnLoad)))){
			scheduleConfig.setExecOnLoad(fomSchedule.execOnLoad()); 
		}

		String taskOverTime = fomSchedule.taskOverTimeString();
		if(StringUtils.isEmpty(taskOverTime) 
				|| !scheduleConfig.setTaskOverTime(Integer.parseInt(valueResolver.resolveStringValue(taskOverTime)))){
			scheduleConfig.setTaskOverTime(fomSchedule.taskOverTime());
		}

		String cancelTaskOnTimeout = fomSchedule.cancelTaskOnTimeoutString();
		if(StringUtils.isEmpty(cancelTaskOnTimeout) 
				|| !scheduleConfig.setCancelTaskOnTimeout(Boolean.parseBoolean(valueResolver.resolveStringValue(cancelTaskOnTimeout)))){
			scheduleConfig.setCancelTaskOnTimeout(fomSchedule.cancelTaskOnTimeout());
		}

		String detectTimeoutOnEachTask = fomSchedule.detectTimeoutOnEachTaskString();
		if(StringUtils.isEmpty(detectTimeoutOnEachTask) 
				|| !scheduleConfig.setDetectTimeoutOnEachTask(Boolean.parseBoolean(valueResolver.resolveStringValue(detectTimeoutOnEachTask)))){
			scheduleConfig.setDetectTimeoutOnEachTask(fomSchedule.detectTimeoutOnEachTask());
		}

		String ignoreExecRequestWhenRunning = fomSchedule.ignoreExecRequestWhenRunningString();
		if(StringUtils.isEmpty(ignoreExecRequestWhenRunning) 
				|| !scheduleConfig.setIgnoreExecRequestWhenRunning(Boolean.parseBoolean(valueResolver.resolveStringValue(ignoreExecRequestWhenRunning)))){
			scheduleConfig.setIgnoreExecRequestWhenRunning(fomSchedule.ignoreExecRequestWhenRunning());
		}

		String enableTaskConflict = fomSchedule.enableTaskConflictString();
		if(StringUtils.isEmpty(enableTaskConflict) 
				|| !scheduleConfig.setEnableTaskConflict(Boolean.parseBoolean(valueResolver.resolveStringValue(enableTaskConflict)))){
			scheduleConfig.setEnableTaskConflict(fomSchedule.enableTaskConflict());
		}

		String enableString = fomSchedule.enableString();
		if(StringUtils.isEmpty(enableString) 
				|| !scheduleConfig.setEnable(Boolean.parseBoolean(valueResolver.resolveStringValue(enableString)))){
			scheduleConfig.setEnable(fomSchedule.enable());
		}
	}

	private void setValue(ScheduleConfig scheduleConfig, ScheduleContext<?> scheduleContext, Object scheduleBean){
		Field[] fields = null;
		if(scheduleBean != null){
			fields = scheduleBean.getClass().getDeclaredFields();
		}else{
			fields = scheduleContext.getClass().getDeclaredFields();
		}

		Map<String, List<Field>> envirment = scheduleConfig.getEnvirment();
		for(Field field : fields){
			Value value = field.getAnnotation(Value.class);
			if(value != null){
				List<String> list = getProperties(value.value());
				String key = null;
				String confValue = null;
				for(String ex : list){
					confValue = valueResolver.resolveStringValue(ex);
					int index = ex.indexOf(":");
					if(index == -1){
						index = ex.indexOf("}");
					}
					key = ex.substring(2, index);

					// 一个环境变量可能赋值给多个Field，一个Field也可能被多个环境变量赋值
					List<Field> fieldList = envirment.get(key);
					if(fieldList == null){
						fieldList = new ArrayList<>();
						envirment.put(key, fieldList);
					}
					fieldList.add(field);
					scheduleConfig.set(key, confValue);
				}
				// 配置集合中的值尽量按照对应属性的类型来（如果可以的话）
				if(list.size() == 1) {
					switch(field.getGenericType().toString()){
					case "short":
					case "class java.lang.Short":
						scheduleConfig.set(key, Short.valueOf(confValue)); break;
					case "int":
					case "class java.lang.Integer":
						scheduleConfig.set(key, Integer.valueOf(confValue)); break;
					case "long":
					case "class java.lang.Long":
						scheduleConfig.set(key, Long.valueOf(confValue)); break;
					case "float":
					case "class java.lang.Float":
						scheduleConfig.set(key, Float.valueOf(confValue)); break;
					case "double":
					case "class java.lang.Double":
						scheduleConfig.set(key, Double.valueOf(confValue)); break;
					case "boolean":
					case "class java.lang.Boolean":
						scheduleConfig.set(key, Boolean.valueOf(confValue)); break;
					}
				}
			}
		}
	}

	static List<String> getProperties(String expression){
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
