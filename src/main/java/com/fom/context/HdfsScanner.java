package com.fom.context;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 * @param <E>
 */
public class HdfsScanner<E extends IHdfsConfig> extends Scanner<E>{

	public HdfsScanner(String name, Config config) {
		super(name, config);
	}
	
	private FileStatus[] scanHdfs(E config){
		FileStatus[] statusArray = null;
		try {
			statusArray = config.getFs().listStatus(new Path(config.getSrcPath()));
		} catch (Exception e) {
			log.error("扫描异常",e);
		}
		return statusArray;
	}

	@Override
	protected List<String> scan(E config) {
		List<String> list = new LinkedList<>();
		FileStatus[] statusArray = scanHdfs(config);
		if(ArrayUtils.isEmpty(statusArray)){
			return list;
		}
		for(FileStatus status : statusArray){
			list.add(status.getPath().getName());
		}
		return list;
	}
	
	@Override
	protected List<String> filter (E config){
		List<String> pathList = new LinkedList<String>();
		FileStatus[] statusArray = scanHdfs(config);
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
			
			if(StringUtils.isBlank(config.getSignalFile())){
				pathList.add(status.getPath().getName());
				continue;
			}
			
			for (FileStatus sub : subStatus){
				if(config.getSignalFile().equals(sub.getPath().getName())){
					pathList.add(status.getPath().getName());
					break;
				}
			}
		}
		return pathList;
	}
}
