package com.fom.context;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author shanhm1991
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
		if(names == null || names.length == 0){
			return list;
		}

		for(String name : names){
			if(config.matchSrc(name)){
				list.add(name);
			}else if(config.delMatchFailFile){
				if(new File(config.srcPath + File.separator + name).delete()){
					log.info("删除不匹配的文件：" + name);
				}else{
					log.info("删除不匹配的文件失败：" + name);
				}
			}
		}
		return list;
	}
}
