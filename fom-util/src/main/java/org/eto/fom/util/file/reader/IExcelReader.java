package org.eto.fom.util.file.reader;

import java.util.List;

/**
 * 
 * @author shanhm
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
	
	/**
	 * set the sheet list and order to be readed by sheetIndex
	 * @param indexList (index start from 1)
	 */
	void setSheetListForReadByIndex(List<Integer> indexList);
	
	/**
	 * set the sheet list and order to be readed by sheetName
	 * @param nameList
	 */
	void setSheetListForReadByName(List<String> nameList);
}
