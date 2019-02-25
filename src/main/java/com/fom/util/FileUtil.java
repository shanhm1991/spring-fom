package com.fom.util;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author shanhm
 *
 */
public class FileUtil {

	public static Set<String> list(String srcUri, FileFilter filter) throws Exception {
		Set<String> set = new HashSet<>();
		if(StringUtils.isBlank(srcUri)){
			return set;
		}

		File[] fileArray = null; 
		if(filter == null){
			fileArray = new File(srcUri).listFiles();
		}else{
			fileArray = new File(srcUri).listFiles(filter);
		}

		if(ArrayUtils.isEmpty(fileArray)){
			return set;
		}
		for(File file : fileArray){
			set.add(file.getPath());
		}
		return set;
	}
}
