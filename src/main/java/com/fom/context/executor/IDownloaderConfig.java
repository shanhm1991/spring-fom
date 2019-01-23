package com.fom.context.executor;

import com.fom.context.config.IConfig;

/**
 * 
 * @author shanhm
 * @date 2019年1月23日
 *
 */
public interface IDownloaderConfig extends IConfig {
	
	/**
	 * 下载文件的目的目录
	 * @return
	 */
	String getDestPath();
	
	/**
	 * 下载文件使用的文件名
	 * @return
	 */
	String getFileName();
	
	/**
	 * 是否先下载到临时目录，然后再移到destPath
	 * @return
	 */
	boolean isWithTemp();
	
	/**
	 * 是否删除源文件
	 * @return
	 */
	boolean isDelSrc();

}
