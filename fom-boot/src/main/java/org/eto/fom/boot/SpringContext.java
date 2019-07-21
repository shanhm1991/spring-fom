package org.eto.fom.boot;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 
 * @author shanhm
 *
 */
@Component
public class SpringContext implements ApplicationContextAware {

	//volatile只能保证引用的变化立即刷新，但系统对这个引用只有一次引用赋值操作
	private static volatile ApplicationContext applicationContext;

	@Override 
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		if(applicationContext != null){
			throw new UnsupportedOperationException();
		}
		applicationContext = context;
	}
	
	/**
	 * 根据bean的id来查找对象
	 * @param id id
	 * @param clzz class
	 * @return bean 
	 * @throws Exception Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getBean(String id, Class<T> clzz) throws Exception {
		return (T)applicationContext.getBean(id);
	}

	/**
	 * 根据bean的class来查找对象
	 * @param clzz class
	 * @return bean
	 */
	public static <T> T getBean(Class<T> clzz){
		return applicationContext.getBean(clzz);
	}

	/**
	 * 根据bean的class来查找所有的对象(包括子类)
	 * @param clzz class
	 * @return bean
	 */
	public static <T> Map<String,T> getBeans(Class<T> clzz){
		return applicationContext.getBeansOfType(clzz);
	}
}
