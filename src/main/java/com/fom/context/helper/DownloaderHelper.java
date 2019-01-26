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
	 * @param sourceUri
	 * @return
	 * @throws Exception
	 */
	InputStream open(String sourceUri) throws Exception;
	
	/**
	 * 根据sourceUri下载文件
	 * @param sourceUri
	 * @param file
	 * @throws Exception
	 */
	void download(String sourceUri, File file) throws Exception;
	
	/**
	 * 根据sourceUri删除文件
	 * @param sourceUri
	 * @return
	 * @throws Exception
	 */
	boolean delete(String sourceUri) throws Exception;
}
