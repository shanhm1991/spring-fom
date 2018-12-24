package com.fom.context;

import java.io.File;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.fom.util.exception.WarnException;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public abstract class Downloader<E extends DownloaderConfig> extends Executor<E>{

	protected Downloader(String name, String path) {
		super(name, path);
	}

	@Override
	final void execute() throws Exception {
		
		download(config);
		
		move(config);
	}
	
	protected abstract void download(final E config) throws Exception;

	protected void move(final E config) throws Exception{
		if(StringUtils.isBlank(config.tempPath)){
			return;
		}
		
		File tempDir = new File(config.tempPath);
		File[] files = tempDir.listFiles();
		if(ArrayUtils.isEmpty(files)){
			return;
		}
		for(File file : files){
			if(!file.renameTo(new File(config.tempPath + File.separator + file.getName()))){
				throw new WarnException("文件移动失败:" + file.getName());
			}
		}
	}
}
