package com.fom.context.executor.helper;

import java.util.List;

import com.fom.context.executor.reader.Reader;

/**
 * 
 * @author shanhm
 * @date 2019年1月23日
 *
 */
public interface ImporterHelper<V> {
	
	/**
	 * 获取对应文件的reader
	 * @param sourceUri
	 * @return
	 * @throws Exception
	 */
	Reader getReader(String sourceUri) throws Exception;

	/**
	 * 解析行数据，并将结果V添加到lineDatas中
	 * @param lineDatas
	 * @param line
	 * @param batchTime
	 * @throws Exception
	 */
	void praseLineData(List<V> lineDatas, String line, long batchTime) throws Exception;

	/**
	 * 批处理行数据解析结果，将lineDatas中的V入库
	 * @param lineDatas
	 * @param batchTime
	 * @throws Exception
	 */
	void batchProcessLineData(List<V> lineDatas, long batchTime) throws Exception;
	
	/**
	 * 根据sourceUri删除文件
	 * @param sourceUri
	 * @return
	 * @throws Exception
	 */
	boolean delete(String sourceUri);
	
	/**
	 * 获取指定sourceUri的文件的大小
	 * @param sourceUri
	 * @return
	 */
	long getFileSize(String sourceUri);
	
	/**
	 * 获取指定sourceUri的文件的名称
	 * @param sourceUri
	 * @return
	 */
	String getFileName(String sourceUri);
}
