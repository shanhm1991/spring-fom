package org.eto.fom.util.file.reader;

import java.io.Closeable;

/**
 * 
 * 读取适配器
 * 
 * @author shanhm
 *
 */
public interface Reader extends Closeable {
	
	public static final String EXCEL_XLS = "xls";

	public static final String EXCEL_XLSX = "xlsx";
	
	/**
	 * 读取下一行
	 * @return ReaderRow
	 * @throws Exception Exception
	 */
	ReaderRow readRow() throws Exception;
	
}
