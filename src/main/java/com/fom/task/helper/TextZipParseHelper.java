package com.fom.task.helper;

import com.fom.task.ZipParseTask;

/**
 * ZipParseTask中需要的操作方法
 * 
 * @author shanhm
 * 
 * @see TxtParseHelper
 * @see ZipParseTask
 */
public interface TextZipParseHelper extends TxtParseHelper {

	/**
	 * 匹配zip的entry名称
	 * @param entryName entryName
	 * @return is matched 
	 */
	boolean matchEntryName(String entryName);
}
