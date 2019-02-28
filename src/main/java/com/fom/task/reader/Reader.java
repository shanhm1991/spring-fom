package com.fom.task.reader;

import java.io.Closeable;

/**
 * 
 * 为ParseTask以及ZipParseTask定制的一个读取适配器
 * 
 * @author shanhm
 *
 */
public interface Reader extends Closeable {
	
	/**
	 * 读取下一行
	 * @return RowData
	 * @throws Exception Exception
	 */
	RowData readRow() throws Exception;
	
}
