package com.fom.util;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class FileUtil {

	/**
	 * 
	 * @param fs
	 * @param srcUri
	 * @param pattern
	 * @param signalName
	 * @return
	 */
	public static final List<String> scan(FileSystem fs, String srcUri, 
			Pattern pattern, String signalName) throws Exception {
		List<String> list = new LinkedList<String>();
		FileStatus[] array = fs.listStatus(new Path(srcUri));
		if(ArrayUtils.isEmpty(array)){
			return list;
		}
		for(FileStatus status : array) {
			String name = status.getPath().getName();
			if(!pattern.matcher(name).find()){
				continue;
			}
			//如果是文件,直接通过
			if(status.isFile()){
				list.add(status.getPath().toString());
				continue;
			}
			//如果是目录,继续校验是否为空目录或者是否存在信号文件
			FileStatus[] subArray = fs.listStatus(status.getPath());
			if(ArrayUtils.isEmpty(subArray)){
				continue;
			}
			if(StringUtils.isBlank(signalName)){
				list.add(status.getPath().toString());
				continue;
			}
			for (FileStatus sub : subArray){
				if(signalName.equals(sub.getPath().getName())){
					list.add(status.getPath().toString());
					break;
				}
			}
		}
		return list;
	}

	public static final List<String> scan(String srcUri, Pattern pattern, boolean isDelMatchFail) throws Exception {
		List<String> list = new LinkedList<>();
		if(srcUri == null){
			return list;
		}
		
		File[] array = new File(srcUri).listFiles();
		if(ArrayUtils.isEmpty(array)){
			return list;
		}
		for(File file : array){
			String name = file.getName();
			if(pattern != null && pattern.matcher(name).find()){
				list.add(file.getPath());
				continue;
			}
			if(isDelMatchFail && !file.delete()){
//				log.warn("删除文件[不匹配]失败:" + name);
			}
		}
		return list;
	}
}
