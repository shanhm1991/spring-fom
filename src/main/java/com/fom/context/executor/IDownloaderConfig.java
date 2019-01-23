package com.fom.context.executor;

/**
 * 
 * @author shanhm
 * @date 2019年1月23日
 *
 */
public interface IDownloaderConfig {
	
	/**
	 * 获取下载文件的资源uri
	 * @return
	 */
	String getUri();
	
	/**
	 * 下载文件的目的目录
	 * @return
	 */
	String getDestPath();
	
	/**
	 * 下载文件使用的文件名
	 * @return
	 */
	String getDestName();
	
	/**
	 * 是否先下载到临时目录，然后再移到destPath
	 * @return
	 */
	boolean isWithTemp();
	
	/**
	 * 下载结束时是否删除源文件
	 * @return
	 */
	boolean isDelSrc();

}
