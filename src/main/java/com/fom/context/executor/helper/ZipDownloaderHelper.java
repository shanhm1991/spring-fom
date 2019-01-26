package com.fom.context.executor.helper;

/**
 * 
 * @author shanhm1991
 *
 */
public interface ZipDownloaderHelper extends DownloaderHelper {

	/**
	 * 根据sourceUri获取资源名称
	 * @param sourceUri
	 * @return
	 */
	String getSourceName(String sourceUri);
}
