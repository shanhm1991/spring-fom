package com.fom.context.scanner;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

import com.fom.context.Scanner;

/**
 * 
 * @author shanhm
 *
 * @param <E>
 */
public class HdfsScanner<E extends HdfsConfig> extends Scanner<E>{

	protected HdfsScanner(String name) {
		super(name);
	}
	
	@Override
	public List<String> scan(String srcUri, E config){
		List<String> list = new LinkedList<String>();
		FileStatus[] array = null;
		try {
			array = config.getFs().listStatus(new Path(srcUri));
		} catch (Exception e) {
			log.error("扫描异常",e);
			return list;
		}
		
		if(ArrayUtils.isEmpty(array)){
			return list;
		}
		
		for(FileStatus status : array) {
			String name = status.getPath().getName();
			if(!config.matchSourceName(name)){
				continue;
			}
			
			//如果是文件,直接通过
			if(status.isFile()){
				list.add(status.getPath().toString());
				continue;
			}
			
			//如果是目录,继续校验是否为空目录或者是否存在信号文件
			FileStatus[] subArray = null;
			try {
				subArray = config.getFs().listStatus(status.getPath());
			} catch (Exception e) {
				log.warn("读取目录失败:" + name, e);
				continue;
			}
			
			if(ArrayUtils.isEmpty(subArray)){
				continue;
			}
			
			if(StringUtils.isBlank(config.getSignalFileName())){
				list.add(status.getPath().toString());
				continue;
			}
			
			for (FileStatus sub : subArray){
				if(config.getSignalFileName().equals(sub.getPath().getName())){
					list.add(status.getPath().toString());
					break;
				}
			}
		}
		return list;
	}
}
