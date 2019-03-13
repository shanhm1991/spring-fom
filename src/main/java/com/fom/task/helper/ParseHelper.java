package com.fom.task.helper;

/**
 * 
 * @author shanhm
 *
 */
public interface ParseHelper {

	/**
	 * 根据sourceUri删除文件
	 * @param sourceUri 资源uri
	 * @return 是否删除成功
	 */
	boolean delete(String sourceUri);
	
	/**
	 * 获取对应sourceUri的资源字节数
	 * @param sourceUri 资源uri
	 * @return 资源字节数
	 */
	long getSourceSize(String sourceUri);
}
