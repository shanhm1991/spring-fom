package com.fom.context.helper;

import java.io.File;
import java.io.InputStream;

import com.fom.context.task.DownloadTask;

/**
 * DownloadTask中需要的具体操作方法
 * 
 * @see DownloadTask
 * 
 * @author shanhm
 * 
 */
public interface DownloadHelper {  
	
	/**
	 * 根据sourceUri打开文件输入流
	 * @param sourceUri 资源uri
	 * @return InputStream
	 * @throws Exception Exception
	 */
	InputStream open(String sourceUri) throws Exception;
	
	/**
	 * 根据sourceUri下载文件
	 * @param sourceUri 资源uri
	 * @param file 下载本地目标文件
	 * @throws Exception Exception
	 */
	void download(String sourceUri, File file) throws Exception;
	
	/**
	 * 根据sourceUri删除文件
	 * @param sourceUri 资源uri
	 * @return 返回码(success:200~207)
	 * @throws Exception Exception
	 */
	int delete(String sourceUri) throws Exception;
}
