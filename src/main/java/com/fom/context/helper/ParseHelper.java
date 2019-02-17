package com.fom.context.helper;

import java.util.List;

import com.fom.context.reader.Reader;
/**
 * ParseTask中需要的具体操作方法
 * 
 * @author shanhm
 * 
 * @see ParseTask
 *
 * @param <V> 行数据解析结果类型
 */
public interface ParseHelper<V> {
	
	/**
	 * 获取对应文件的reader
	 * @param sourceUri 资源uri
	 * @return Reader
	 * @throws Exception Exception
	 */
	Reader getReader(String sourceUri) throws Exception;

	/**
	 * 解析行数据，将line解析成结果V并添加到lineDatas中
	 * @param lineDatas lineDatas
	 * @param line line
	 * @param batchTime batchTime
	 * @throws Exception Exception
	 */
	void praseLineData(List<V> lineDatas, String line, long batchTime) throws Exception;

	/**
	 * 批处理行数据解析结果
	 * @param lineDatas lineDatas
	 * @param batchTime batchTime
	 * @throws Exception Exception
	 */
	void batchProcessLineData(List<V> lineDatas, long batchTime) throws Exception;
	
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
