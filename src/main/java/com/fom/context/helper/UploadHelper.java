package com.fom.context.helper;

import java.io.File;

/**
 * 
 * @author shanhms
 *
 */
public interface UploadHelper {
	
	/**
	 * 上传文件
	 * @param file 文件
	 * @param destUri 目的uri
	 * @return 返回码(success:200~207)
	 * @throws Exception Exception
	 */
	int upload(File file, String destUri) throws Exception;

}
