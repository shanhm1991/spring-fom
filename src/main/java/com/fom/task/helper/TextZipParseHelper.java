package com.fom.task.helper;

import com.fom.task.ZipParseTask;

/**
 * ZipParseTask中需要的操作方法
 * 
 * @author shanhm
 * 
 * @see TextParseHelper
 * @see ZipParseTask
 *
 * @param <V> 行数据解析结果类型
 */
public interface TextZipParseHelper<V> extends TextParseHelper<V> {

	/**
	 * 匹配zip的entry名称
	 * @param entryName entryName
	 * @return is matched 
	 */
	boolean matchEntryName(String entryName);
}
