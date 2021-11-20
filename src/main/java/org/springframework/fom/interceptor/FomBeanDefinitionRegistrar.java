package org.springframework.fom.interceptor;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.fom.FomBeanPostProcessor;
import org.springframework.fom.FomScheduleStarter;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.annotation.EnableFom;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.fom.support.FomAdvice;
import org.springframework.fom.support.controller.FomController;
import org.springframework.fom.support.service.FomServiceImpl;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class FomBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar{

	private static AtomicBoolean registed = new AtomicBoolean(false);

	@Override
	public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
		if(!registed.compareAndSet(false, true)){
			return;
		}
		
		AnnotationAttributes attrs = AnnotationAttributes.fromMap(meta.getAnnotationAttributes(EnableFom.class.getName()));
		if((boolean)attrs.get("enableFomView")){
			// 注册FomController
			RootBeanDefinition fomController = new RootBeanDefinition(FomController.class);
			registry.registerBeanDefinition("fomController", fomController); 
			
			// 注册FomAdvice
			RootBeanDefinition fomAdvice = new RootBeanDefinition(FomAdvice.class);
			registry.registerBeanDefinition("fomAdvice", fomAdvice); 
			
			// 注册FomServiceImpl
			RootBeanDefinition fomServiceImpl = new RootBeanDefinition(FomServiceImpl.class);
			registry.registerBeanDefinition("fomService", fomServiceImpl); 
		}

		// 注册SchedulePostProcessor
		RootBeanDefinition fomBeanPostProcessor = new RootBeanDefinition(FomBeanPostProcessor.class);
		registry.registerBeanDefinition("schedulePostProcessor", fomBeanPostProcessor); 

		// 注册FomScheduleStarter
		RootBeanDefinition fomScheduleStarter = new RootBeanDefinition(FomScheduleStarter.class);
		registry.registerBeanDefinition("fomScheduleStarter", fomScheduleStarter); 

		// 注册FomBeanDefinition
		String[] beanNames = registry.getBeanDefinitionNames();
		Class<?> clazz;
		for(String beanName : beanNames){
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			String className = beanDefinition.getBeanClassName();
			if(className != null){
				try {
					clazz = Class.forName(className);
				} catch (ClassNotFoundException e) {
					throw new ApplicationContextException("", e);
				}

				FomSchedule fomSchedule = clazz.getAnnotation(FomSchedule.class);
				if(fomSchedule != null){
					parseFomSchedule(beanName, clazz, beanDefinition, fomSchedule, registry);
				}
			}
		}
	}

	public void parseFomSchedule(String beanName, Class<?> clazz, BeanDefinition beanDefinition, FomSchedule fomSchedule, BeanDefinitionRegistry registry){
		if(ScheduleContext.class.isAssignableFrom(clazz)){
			beanDefinition.getPropertyValues().add("scheduleName", beanName);
			registry.registerAlias(beanName,  "$" + beanName); 
		}else{
			RootBeanDefinition fomBeanDefinition = new RootBeanDefinition(ScheduleContext.class);
			fomBeanDefinition.getPropertyValues().add("scheduleBeanName", beanName);
			fomBeanDefinition.getPropertyValues().add("scheduleName", "$" + beanName);
			registry.registerBeanDefinition("$" + beanName, fomBeanDefinition); 
		}
	}
}
