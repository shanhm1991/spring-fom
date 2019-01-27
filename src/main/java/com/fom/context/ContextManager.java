package com.fom.context;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.reflections.Reflections;

import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 */
public class ContextManager {

	private static final Logger LOG = LoggerFactory.getLogger("fom");

	private static volatile ServletContext scontext;
	
	private static Map<String,Context<? extends Config>> contextMap = new ConcurrentHashMap<>();

	public final static void setContext(ServletContext context) {
		if(scontext == null){
			scontext = context;
		}
	}


	/**
	 * Return the real path for a given virtual path, if possible; otherwise return <code>null</code>.
	 * @param location
	 * @return
	 * @throws Exception
	 */
	public static final String getContextPath(String path) {
		return scontext.getRealPath(getEnvStr(path));
	}


	/**
	 * 获取带环境变量的字符串值，如${webapp.root}/test
	 * @param val
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static final String getEnvStr(String val) throws IllegalArgumentException {
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
						String recursiveReplacement = getEnvStr(replacement);
						buffer.append(recursiveReplacement);
					}
					i = k + DELIM_STOP_LEN;
				}
			}
		}
	}

	/**
	 * 
	 * @param packages 默认扫描com.fom,另外包括配置的路径
	 */
	@SuppressWarnings("unchecked")
	static void init(List<String> pckageList){
		Set<Class<?>> contextSet = new HashSet<>();
		for(String pack : pckageList){
			Reflections reflections = new Reflections(pack);
			contextSet.addAll(reflections.getTypesAnnotatedWith(FomContext.class));
		}

		for(Class<?> clazz : contextSet){
			if(!Context.class.isAssignableFrom(clazz)){
				LOG.error(clazz + "没有继承com.fom.context.Context, 初始化失败."); 
				continue;
			}
			FomContext fc = clazz.getAnnotation(FomContext.class);
			String name = fc.name();
			if(StringUtils.isBlank(name)){
				LOG.error(clazz + "没有指定注解name属性, 初始化失败."); 
			}

			Context<? extends Config> context = null;
			try {
				context = (Context<? extends Config>)clazz.newInstance();
			} catch (Exception e) {
				LOG.error("[" + name + "]" + clazz + "初始化失败", e);
				continue;
			} 
			context.start();
			contextMap.put(name, context);
		}
	}

}
