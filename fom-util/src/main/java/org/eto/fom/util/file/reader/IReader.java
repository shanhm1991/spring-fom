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
	
	public static final String EXCEL_XLS = "xls";

	public static final String EXCEL_XLSX = "xlsx";
	
	/**
	 * 读取下一行
	 * @return ReaderRow
	 * @throws Exception Exception
	 */
	IRow readRow() throws Exception;
	
}
