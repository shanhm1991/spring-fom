package com.fom.task.helper;

import java.io.InputStream;
import java.util.List;

/**
 * 
 * @author shanhm
 *
 */
public interface ExcelParseHelper {

	/**
	 * 获取对应文件的InputStream
	 * @param sourceUri 资源uri
	 * @return InputStream
	 * @throws Exception Exception
	 */
	InputStream getInputStream(String sourceUri) throws Exception;

	/**
	 * Excel类型  xls or xlsx
	 * @return  xls or xlsx
	 */
	String getExcelType();

	/**
	 * 过滤需要处理的sheet页
	 * @param sheetIndex sheetIndex
	 * @param sheetName sheetName
	 * @return boolean
	 */
	boolean sheetFilter(int sheetIndex, String sheetName);

	/**
	 * 自定义sheet处理顺序
	 * @param sheetRangeList 原sheet顺序
	 * @return 重排序后sheet顺序
	 */
	List<String> reRangeSheet(List<String> sheetRangeList);
}
