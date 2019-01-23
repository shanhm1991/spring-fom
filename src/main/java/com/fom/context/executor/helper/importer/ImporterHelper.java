package com.fom.context.executor.helper.importer;

import java.util.List;


/**
 * 
 * @author shanhm
 * @date 2019年1月23日
 *
 */
public interface ImporterHelper<V> {

	/**
	 * 解析行数据
	 * @param lineDatas
	 * @param line
	 * @param batchTime
	 * @throws Exception
	 */
	void praseLineData(List<V> lineDatas, String line, long batchTime) throws Exception;

	/**
	 * 批处理行数据解析结果
	 * @param lineDatas
	 * @param batchTime
	 * @throws Exception
	 */
	void batchProcessLineData(List<V> lineDatas, long batchTime) throws Exception;
	
	/**
	 * 根据uri删除文件
	 * @param uri
	 * @return
	 * @throws Exception
	 */
	boolean delete(String uri);
	
	/**
	 * 获取指定uri的文件的大小
	 * @param uri
	 * @return
	 */
	long getFileSize(String uri);
	
	/**
	 * 获取指定uri的文件的名称
	 * @param uri
	 * @return
	 */
	String getFileName(String uri);
}
