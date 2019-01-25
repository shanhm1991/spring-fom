package com.fom.context.executor;


/**
 * 
 * @author shanhm
 *
 */
public interface ZipDownloaderConfig extends DownloaderConfig {
	
	/**
	 * 下载打包zip时的最大文件数
	 * @return
	 */
	int getEntryMax();
	
	/**
	 * 下载打包zip时的最大字节数
	 * @return
	 */
	long getSizeMax();
}
