package com.fom.util;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * 
 * @author shanhm
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
	public static final <T> T getBean(String id, Class<T> clzz) throws Exception {
        return (T)applicationContext.getBean(id);
    }
     
    /**
     * 根据bean的class来查找对象
     * @param c
     * @return
     */
    public static final <T> T getBean(Class<T> c){
        return applicationContext.getBean(c);
    }
     
    /**
     * 根据bean的class来查找所有的对象(包括子类)
     * @param c
     * @return
     */
	public static final <T> Map<String,T> getBeans(Class<T> c){
        return applicationContext.getBeansOfType(c);
    }
}
