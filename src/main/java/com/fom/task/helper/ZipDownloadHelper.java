package com.fom.task.helper;

import java.util.zip.ZipOutputStream;

import com.fom.task.ZipDownloadTask;

/**
 * ZipDownloadTask中需要的具体操作方法
 * 
 * @see DownloadHelper
 * @see ZipDownloadTask
 * 
 * @author shanhm
 *
 */
public interface ZipDownloadHelper extends DownloadHelper {

	/**
	 * 根据sourceUri获取资源名称
	 * @param sourceUri 资源uri
	 * @return source name
	 */
	String getSourceName(String sourceUri);
	
	/**
	 * 将uri对应的资源写入zipOutStream
	 * @param name 写入zip时使用的文件名称
	 * @param sourceUri 资源uri
	 * @param zipOutStream zip输出流
	 * @return 写入字节数
	 * @throws Exception Exception
	 */
	long zipEntry(String name, String sourceUri, ZipOutputStream zipOutStream) throws Exception;
}
