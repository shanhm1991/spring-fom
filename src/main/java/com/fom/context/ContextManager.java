package com.fom.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 */
public class ContextManager {
	
	private static volatile ServletContext scontext;
	
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
	
	private static final Logger LOG = LoggerFactory.getLogger("context");
	
	private static Map<String,Context> contextMap = new ConcurrentHashMap<>();
	
	public static boolean register(Context context){
		if(context == null){
			return false;
		}
		if(contextMap.containsKey(context.name)){
			LOG.warn("context[" + context.name + "]已经存在,取消加载.");
			return false;
		}
		contextMap.put(context.name, context);
		LOG.info("加载context[" + context.name + "]");
		return true;
	}

}
