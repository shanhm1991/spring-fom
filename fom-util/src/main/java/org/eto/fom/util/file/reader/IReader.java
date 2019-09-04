package org.eto.fom.util.file.reader;

import java.io.Closeable;

/**
 * 
 * 读取适配器
 * 
 * @author shanhm
 *
 */
public interface IReader extends Closeable {
	
	/**
	 * 读取下一行
	 * @return ReaderRow
	 * @throws Exception Exception
	 */
	IRow readRow() throws Exception;
	
}
