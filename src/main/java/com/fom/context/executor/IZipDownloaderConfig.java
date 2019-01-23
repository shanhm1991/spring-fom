package com.fom.context.executor;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public interface IZipDownloaderConfig extends IDownloaderConfig {
	
	int getEntryMax();
	
	long getSizeMax();

}
