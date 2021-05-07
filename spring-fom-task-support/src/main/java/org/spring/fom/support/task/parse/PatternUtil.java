package org.spring.fom.support.task.parse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
class PatternUtil {
	
	private static Map<String, Pattern> patternMap = new ConcurrentHashMap<>();
	
	private static Pattern get(String regex){
		Pattern pattern = patternMap.get(regex);
		if(pattern != null){
			return pattern;
		}
		pattern = Pattern.compile(regex);
		patternMap.put(regex, pattern);
		return pattern;
	}
	
	public static boolean match(String regex, String target){
		if(StringUtils.isBlank(regex)){
			return true;
		}
		return get(regex).matcher(target).matches();
	}

}
