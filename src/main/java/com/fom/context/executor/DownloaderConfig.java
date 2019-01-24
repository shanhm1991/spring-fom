package com.fom.context.executor;

/**
 * 
 * @author shanhm
 * @date 2019年1月23日
 *
 */
public interface DownloaderConfig {
	
	/**
	 * 下载文件的目的目录
	 * @return
	 */
	String getDestPath();
	
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
	
	/**
	 * 根据uri获取下载资源文件的名称
	 * @param uri
	 * @return
	 */
	String getSourceName(String uri);

}
