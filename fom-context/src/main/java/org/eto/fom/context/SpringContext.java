package org.eto.fom.context;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

/**
 * 
 * @author shanhm
 *
 */
@Component
public class SpringContext implements ApplicationContextAware, EmbeddedValueResolverAware {

	//仅由main初始化一次
	private static volatile ApplicationContext applicationContext;

	private static volatile StringValueResolver stringValueResolver;

	@Override 
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		SpringContext.applicationContext = applicationContext;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver stringValueResolver) {
		SpringContext.stringValueResolver = stringValueResolver;
	}

	/**
	 * 获取spring环境配置
	 * @param name
	 * @return
	 */
	public static String getPropertiesValue(String name) {
		return stringValueResolver.resolveStringValue(name);
	}
	
	/**
	 * 获取应用下的资源路径
	 * @param path
	 * @return
	 * @throws IOException 
	 */
	public static String getPath(String path) throws IOException{
		Resource resource = applicationContext.getResource(path);
		if(resource != null){
			return resource.getFile().getPath();
		}
		return "";
	}
	
	/**
	 * 获取应用下的资源
	 * @param path
	 * @return
	 */
	public static Resource getResource(String path){
		return applicationContext.getResource(path);
	}
	
	/**
	 * 获取应用下的资源
	 * @param path 
	 * @return
	 * @throws IOException 
	 */
	public static Resource[] getResources(String path) throws IOException{
		return applicationContext.getResources(path);
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
