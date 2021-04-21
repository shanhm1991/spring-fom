package org.springframework.fom.interceptor;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.annotation.FomSchedule;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class FomRegistrar implements ImportBeanDefinitionRegistrar{

	private static AtomicBoolean registed = new AtomicBoolean(false);

	@Override
	public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
		if(!registed.compareAndSet(false, true)){
			return;
		}

		String[] beanNames = registry.getBeanDefinitionNames();
		Class<?> clazz;
		for(String beanName : beanNames){
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			String className = beanDefinition.getBeanClassName();
			try {
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new ApplicationContextException("", e);
			}

			FomSchedule fomSchedule = clazz.getAnnotation(FomSchedule.class);
			if(fomSchedule != null){
				parseFomSchedule(beanName, clazz, fomSchedule, registry);
			}
		}
	}

	public void parseFomSchedule(String beanName, Class<?> clazz, FomSchedule fomSchedule, BeanDefinitionRegistry registry){
		
		// 配置 spring配置放到map中
		
		if(ScheduleContext.class.isAssignableFrom(clazz)){
			registry.registerAlias(beanName,  "$" + beanName); 
		}else{
			RootBeanDefinition fomBeanDefinition = new RootBeanDefinition(ScheduleContext.class);
			fomBeanDefinition.getPropertyValues().add("scheduleBeanName", beanName);
			
			registry.registerBeanDefinition("$" + beanName, fomBeanDefinition); 
		}
	}
}
