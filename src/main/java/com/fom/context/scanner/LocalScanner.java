package com.fom.context.scanner;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.fom.context.IConfig;
import com.fom.context.Scanner;

/**
 * 
 * @author shanhm
 *
 * @param <E>
 */
public class LocalScanner<E extends IConfig> extends Scanner<E> {

	protected LocalScanner(String name) {
		super(name);
	}

	@Override
	public List<String> scan(String srcUri, E config) {
		List<String> list = new LinkedList<>();
		File[] array = new File(srcUri).listFiles();
		if(ArrayUtils.isEmpty(array)){
			return list;
		}
		
		for(File file : array){
			String name = file.getName();
			if(config.matchSourceName(name)){
				list.add(file.getPath());
				continue;
			}
			if(config.isDelMatchFailFile() && !file.delete()){
				log.warn("删除文件[不匹配]失败:" + name);
			}
		}
		return list;
	}
}
