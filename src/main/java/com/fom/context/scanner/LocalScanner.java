package com.fom.context.scanner;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.fom.context.IConfig;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class LocalScanner<E extends IConfig> extends Scanner<E> {

	protected LocalScanner(String name) {
		super(name);
	}

	@Override
	public List<String> scan(E config) {
		List<String> list = new LinkedList<>();

		String[] names = new File(config.getUri()).list();
		if(ArrayUtils.isEmpty(names)){
			return list;
		}
		list.addAll(Arrays.asList(names));
		return list;
	}

	@Override
	public List<String> filter(E config) {
		List<String> list = scan(config);
		if(list.isEmpty()){
			return list;
		}
		
		Iterator<String> it = list.iterator();
		while(it.hasNext()){
			String name = it.next();
			if(config.matchSrc(name)){
				continue;
			}
			
			it.remove();
			if(config.isDelMatchFailFile() 
					&& !new File(config.getUri() + File.separator + name).delete()){
				log.warn("删除文件失败[不匹配]:" + name);
			}
		}
		return list;
	}
}
