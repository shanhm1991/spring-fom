package com.fom.context.scanner;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

import com.fom.context.config.IHdfsConfig;
import com.fom.util.HdfsUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class HdfsScanner<E extends IHdfsConfig> extends Scanner<E>{

	protected HdfsScanner(String name, E config) {
		super(name, config);
	}
	
	@Override
	public List<String> scan(E config) {
		try{
			return HdfsUtil.listName(config.getFs(), config.getUri(), null);
		} catch (Exception e) {
			log.error("扫描异常",e);
		}
		return new LinkedList<String>();
	}
	
	@Override
	public List<String> filter (E config){
		List<String> pathList = new LinkedList<String>();
		FileStatus[] statusArray = null;
		try {
			statusArray = config.getFs().listStatus(new Path(config.getUri()));
		} catch (Exception e) {
			log.error("扫描异常",e);
			return pathList;
		}
		
		if(ArrayUtils.isEmpty(statusArray)){
			return pathList;
		}
		
		for(FileStatus status : statusArray) {
			if(!config.matchSrc(status.getPath().getName())){
				continue;
			}
			
			//如果是文件直接通过
			if(status.isFile()){
				pathList.add(status.getPath().getName());
				continue;
			}
			
			//如果是目录继续校验是否为空目录或者是否存在信号文件
			FileStatus[] subStatus = null;
			try {
				subStatus = config.getFs().listStatus(status.getPath());
			} catch (Exception e) {
				log.warn("读取目录失败:" + status.getPath().getName(), e);
				continue;
			}

			if(ArrayUtils.isEmpty(subStatus)){
				continue;
			}
			
			if(StringUtils.isBlank(config.getSignalFileName())){
				pathList.add(status.getPath().getName());
				continue;
			}
			
			for (FileStatus sub : subStatus){
				if(config.getSignalFileName().equals(sub.getPath().getName())){
					pathList.add(status.getPath().getName());
					break;
				}
			}
		}
		return pathList;
	}
}
