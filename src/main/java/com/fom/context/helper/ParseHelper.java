package com.fom.context.helper;

import java.util.List;

import com.fom.context.reader.Reader;
import com.fom.context.task.ParseTask;
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
	 * 将行字段数据映射成对应的java bean或者map,并添加到数据集batchData中<br>
	 * 当batchData中数量达到阈值时，执行批处理操作
	 * @param columns 列字段值
	 * @param batchData 批处理数据集
	 * @param batchTime 批处理时间
	 * @throws Exception Exception
	 */
	void praseLineData(List<String> columns, List<V> batchData, long batchTime) throws Exception;

	/**
	 * 批处理行数据
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
