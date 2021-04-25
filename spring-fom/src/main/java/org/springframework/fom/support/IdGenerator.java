package org.springframework.fom.support;

import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class IdGenerator {
	
	private AtomicInteger index = new AtomicInteger(0);

	/**
	 * 获取固定长度的id
	 * @param prefix
	 * @param suffix
	 * @param dateFormat 时间格式
	 * @param indexLimit index上限
	 * @return
	 */
	public String generateIdWithDate(String prefix, String suffix, String dateFormat, int indexLimit){
		Assert.isTrue(!StringUtils.isEmpty(dateFormat), "dateFormat cannot be empty.");
		Assert.isTrue(indexLimit > 0, "indexLimit must greater than 0.");
		
		String date = new SimpleDateFormat(dateFormat).format(System.currentTimeMillis());
		int limitLen = String.valueOf(indexLimit).length();
		
		StringBuilder builder = new StringBuilder();
		if(!StringUtils.isEmpty(prefix)){ 
			builder.append(prefix);
		}
		
		builder.append(date);
		
		if(!StringUtils.isEmpty(suffix)){ 
			builder.append(suffix);
		}
		
		String currentIndex = String.valueOf(index.incrementAndGet() % indexLimit);
		int currentLen = currentIndex.length(); 
		
		int currentLimit = 1;
		for(int i = 1; i < currentLen; i++){
			currentLimit *= 10;
		}
		for(int i = currentIndex.length(); i < limitLen; i++){
			currentLimit *= 10;
			if(currentLimit < indexLimit){
				builder.append('0');
			}
		}
		return builder.append(currentIndex).toString();
	}
	
}
