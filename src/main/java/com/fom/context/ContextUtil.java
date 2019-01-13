package com.fom.context;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;

/**
 * 
 * @author shanhm
 * @date 2019年1月10日
 *
 */
public class ContextUtil extends AbstractRefreshableWebApplicationContext {
	
	public static final ContextUtil INSTANCE = new ContextUtil();
	
	
	/**
	 * 借助spring的方式获取配置的文件
	 * @param location
	 * @return
	 * @throws Exception
	 */
	public static final File getResourceFile(String location) throws Exception {
		
		System.out.println(parseEnvStr(location));
		
		System.out.println(INSTANCE.getResource(parseEnvStr(location)).exists()); 
		return INSTANCE.getResource(parseEnvStr(location)).getFile();
	}
	

	/**
	 * 获取带环境变量的字符串值，如${webapp.root}/test
	 * @param val
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static final String parseEnvStr(String val) throws IllegalArgumentException {
		String DELIM_START = "${";
		char   DELIM_STOP  = '}';
		int DELIM_START_LEN = 2;
		int DELIM_STOP_LEN  = 1;
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		int j, k;
		while(true) {
			j = val.indexOf(DELIM_START, i);
			if(j == -1) {
				if(i==0) {
					return val;
				} else { 
					buffer.append(val.substring(i, val.length()));
					return buffer.toString();
				}
			} else {
				buffer.append(val.substring(i, j));
				k = val.indexOf(DELIM_STOP, j);
				if(k == -1) {
					throw new IllegalArgumentException('"' 
							+ val + "\" has no closing brace. Opening brace at position " + j + '.');
				} else {
					j += DELIM_START_LEN;
					String key = val.substring(j, k);
					String replacement = System.getProperty(key);
					if(replacement != null) {
						String recursiveReplacement = parseEnvStr(replacement);
						buffer.append(recursiveReplacement);
					}
					i = k + DELIM_STOP_LEN;
				}
			}
		}
	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory arg0) throws BeansException, IOException {
		
	}
	
}
