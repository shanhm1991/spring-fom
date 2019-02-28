package com.fom.task.helper;

import com.fom.task.ParseTask;
import com.fom.task.reader.Reader;
/**
 * ParseTask中需要的具体操作方法
 * 
 * @author shanhm
 * 
 * @see ParseTask
 *
 * @param <V> 行数据解析结果类型
 */
public interface TextParseHelper<V> extends ParseHelper<V> {
	
	/**
	 * 获取对应文件的reader
	 * @param sourceUri 资源uri
	 * @return Reader
	 * @throws Exception Exception
	 */
	Reader getReader(String sourceUri) throws Exception;
	
}
