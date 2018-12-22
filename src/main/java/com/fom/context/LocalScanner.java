package com.fom.context;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 * @param <E>
 */
public class LocalScanner<E extends Config> extends Scanner<E> {

	public LocalScanner(String name, Config config) {
		super(name, config);
	}

	@Override
	protected List<String> scan(E config) {
		List<String> list = new LinkedList<>();

		String[] names = new File(config.srcPath).list();
		if(ArrayUtils.isEmpty(names)){
			return list;
		}
		list.addAll(Arrays.asList(names));
		return list;
	}

	@Override
	protected List<String> filter(E config) {
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
			if(config.delMatchFailFile && !new File(config.srcPath + File.separator + name).delete()){
				log.warn("删除文件失败[不匹配]:" + name);
			}
		}
		return list;
	}
}
