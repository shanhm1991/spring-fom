package com.fom.modules.downloader.demo.ml;

import java.io.File;

import org.apache.commons.lang.ArrayUtils;

import com.fom.context.HdfsZipDownloader;
import com.fom.context.HdfsZipDownloaderConfig;
import com.fom.util.exception.WarnException;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
public class mlDownLoader extends HdfsZipDownloader<HdfsZipDownloaderConfig>{
	
	public mlDownLoader(String name, String path) {
		super(name, path);
	}

	@Override
	protected void move(final HdfsZipDownloaderConfig config) throws WarnException{ 
		File tempDir = new File(subTempPath);
		File[] files = tempDir.listFiles();
		if(ArrayUtils.isEmpty(files)){
			return;
		}
		for(File file : files){
			String name = file.getName().toUpperCase().replace(".ZIP", ".zip");
			if(!file.renameTo(new File(config.getDestPath() + File.separator + name))){
				throw new WarnException("文件移动失败:" + file.getName()); 
			}
		}
	}
}
