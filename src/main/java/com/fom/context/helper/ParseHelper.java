package com.fom.context.helper;

import java.util.List;

import com.fom.context.reader.RowData;

public interface ParseHelper<V> {

	/**
	 * 将行字段数据映射成对应的bean或者map
	 * @param rowData
	 * @param batchTime 批处理时间
	 * @return 映射结果V列表
	 * @throws Exception Exception
	 */
	List<V> parseRowData(RowData rowData, long batchTime) throws Exception;

	/**
	 * 批处理行数据
	 * @param batchData batchData
	 * @param batchTime batchTime
	 * @throws Exception Exception
	 */
	void batchProcess(List<V> batchData, long batchTime) throws Exception;
	
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
