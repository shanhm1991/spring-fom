package com.fom.context.helper;

import java.util.List;

import com.fom.context.reader.Reader;

/**
 * 
 * @author shanhm
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
	 * 获取对应sourceUri的资源大小
	 * @param sourceUri
	 * @return
	 */
	long getSourceSize(String sourceUri);
	
}
