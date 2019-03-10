package com.fom.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author shanhm
 *
 */
public class FileUtil {

	public static List<String> list(String srcUri, FileFilter filter) throws Exception {
		List<String> list = new ArrayList<>();
		if(StringUtils.isBlank(srcUri)){
			return list;
		}

		File[] fileArray = null; 
		if(filter == null){
			fileArray = new File(srcUri).listFiles();
		}else{
			fileArray = new File(srcUri).listFiles(filter);
		}

		if(ArrayUtils.isEmpty(fileArray)){
			return list;
		}
		for(File file : fileArray){
			list.add(file.getPath());
		}
		return list;
	}
}
