package com.fom.context.helper;

/**
 * ZipParseTask中需要的操作方法
 * 
 * @author shanhm
 * 
 * @see ParseHelper
 * @see ZipParseTask
 *
 * @param <V> 行数据解析结果类型
 */
public interface ZipParseHelper<V> extends ParseHelper<V> {

	/**
	 * 匹配zip的entry名称
	 * @param entryName entryName
	 * @return is matched 
	 */
	boolean matchEntryName(String entryName);
}
