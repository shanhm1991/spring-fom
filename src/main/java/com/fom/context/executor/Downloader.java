package com.fom.context.executor;

import java.io.File;

import com.fom.context.Context;
import com.fom.context.config.DownloaderConfig;
import com.fom.context.exception.WarnException;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public abstract class Downloader<E extends DownloaderConfig> extends Context<E>{
	
	protected Downloader(String name, String path) {
		super(name, path);
	}

	@Override
	protected final void exec(E config) throws Exception {
		
		download(config);
		
		move(config);
	}
	
	protected abstract void download(final E config) throws Exception;

	protected void move(final E config) throws Exception{
		if(!config.isWithTemp()){
			return;
		}
		File file = new File(config.getTempPath() + File.separator + srcName);
		if(file.exists() && !file.renameTo(new File(config.getDestPath() + File.separator + srcName))){
			throw new WarnException("文件移动失败:" + file.getName()); 
		}
	}
}
