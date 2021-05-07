package org.spring.fom.support.task.reader;

import java.util.List;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public interface IExcelReader extends IReader {

	public static final String EXCEL_XLS = "xls";

	public static final String EXCEL_XLSX = "xlsx";
	
	/**
	 * read next row
	 * @return 
	 * @throws Exception Exception
	 */
	ExcelRow readRow() throws Exception;
	
	/**
	 * read next sheet
	 * @return if the current sheet has remaining then return the rest, otherwise return the data of next sheet
	 * @throws Exception
	 */
	List<ExcelRow> readSheet() throws Exception;
	
	/**
	 * set sheet filter
	 * @param sheetFilter
	 */
	void setSheetFilter(ExcelSheetFilter sheetFilter);
	
}
