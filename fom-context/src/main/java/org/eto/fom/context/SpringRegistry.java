package org.eto.fom.context;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author shanhm
 *
 */
@Configuration
public class SpringRegistry {

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	@Autowired
	DefaultListableBeanFactory defaultListableBeanFactory;
	
	public void regist(String name, Object obj){
		defaultListableBeanFactory.registerSingleton(name, obj);
		beanFactory.autowireBean(obj);
	}

	public static void main(String[] args) {
		ConcurrentMap<String, String> valueMap = new ConcurrentHashMap<>();
		valueMap.put("a", null);
	}
}
