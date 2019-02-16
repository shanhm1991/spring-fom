package com.fom.context.helper;

import java.util.zip.ZipOutputStream;

/**
 * 
 * @author shanhm
 *
 */
public interface ZipDownloadHelper extends DownloadHelper {

	/**
	 * 根据sourceUri获取资源名称
	 * @param sourceUri sourceUri
	 * @return source name
	 */
	String getSourceName(String sourceUri);
	
	/**
	 * 将uri对应的资源写入zipOutStream
	 * @param name name
	 * @param uri 资源uri
	 * @param zipOutStream ZipOutputStream
	 * @return 写入字节数
	 * @throws Exception Exception
	 */
	long zipEntry(String name, String uri, ZipOutputStream zipOutStream) throws Exception;
}
