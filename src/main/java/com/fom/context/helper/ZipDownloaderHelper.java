package com.fom.context.helper;

/**
 * 
 * @author shanhm
 *
 */
public interface ZipDownloaderHelper extends DownloaderHelper {

	/**
	 * 根据sourceUri获取资源名称
	 * @param sourceUri sourceUri
	 * @return source name
	 */
	String getSourceName(String sourceUri);
}
