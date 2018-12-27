package com.fom.util;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
@Service
public class SpringUtil implements ApplicationContextAware {
	
	private static ApplicationContext applicationContext;
	
	@SuppressWarnings("static-access")
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
     * 根据bean的id来查找对象
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
	public static <T> T getBeanById(String id, Class<T> clzz) throws Exception {
        return (T)applicationContext.getBean(id);
    }
     
    /**
     * 根据bean的class来查找对象
     * @param c
     * @return
     */
    public static Object getBeanByClass(Class<?> c){
        return applicationContext.getBean(c);
    }
     
    /**
     * 根据bean的class来查找所有的对象(包括子类)
     * @param c
     * @return
     */
    @SuppressWarnings("rawtypes")
	public static Map getBeansByClass(Class<?> c){
        return applicationContext.getBeansOfType(c);
    }
}
