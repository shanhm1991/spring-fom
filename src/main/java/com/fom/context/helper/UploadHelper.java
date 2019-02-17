package com.fom.context.helper;

import java.io.File;

/**
 * UploadTask中需要的具体操作方法
 * 
 * @author shanhms
 * 
 * @see UploadTask
 *
 */
public interface UploadHelper {
	
	/**
	 * 上传文件
	 * @param file 待上传文件
	 * @param destUri 目的uri
	 * @return 返回码(success:200~207)
	 * @throws Exception Exception
	 */
	int upload(File file, String destUri) throws Exception;

}
