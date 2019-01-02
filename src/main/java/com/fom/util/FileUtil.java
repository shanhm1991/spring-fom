package com.fom.util;

import java.io.File;

import org.apache.commons.lang.ArrayUtils;

import com.fom.util.exception.WarnException;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class FileUtil {
	
	public static final void moveTemp(String temp, String dest, boolean delTemp) throws WarnException{ 
		File tempDir = new File(temp);
		File[] files = tempDir.listFiles();
		if(ArrayUtils.isEmpty(files)){
			return;
		}
		for(File file : files){
			if(!file.renameTo(new File(dest + File.separator + file.getName()))){
				throw new WarnException("文件移动失败:" + file.getName());
			}
		}
		if(delTemp && !tempDir.delete()){
			throw new WarnException("删除临时目录失败.");
		}
	}

}
