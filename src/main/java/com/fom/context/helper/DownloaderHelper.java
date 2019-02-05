package com.fom.context.helper;

import java.io.File;
import java.io.InputStream;

/**
 * 
 * @author shanhm
 *
 */
public interface DownloaderHelper {  
	
	/**
	 * 根据sourceUri打开文件输入流
	 * @param sourceUri sourceUri
	 * @return InputStream
	 * @throws Exception Exception
	 */
	InputStream open(String sourceUri) throws Exception;
	
	/**
	 * 根据sourceUri下载文件
	 * @param sourceUri sourceUri
	 * @param file dest file
	 * @throws Exception Exception
	 */
	void download(String sourceUri, File file) throws Exception;
	
	/**
	 * 根据sourceUri删除文件
	 * @param sourceUri sourceUri
	 * @return is delete success
	 * @throws Exception Exception
	 */
	boolean delete(String sourceUri) throws Exception;
}
