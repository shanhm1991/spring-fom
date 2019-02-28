package com.fom.context.helper;

import java.io.InputStream;

/**
 * 
 * @author shanhm
 *
 */
public interface ExcelParseHelper<V> extends ParseHelper<V> {

	/**
	 * 获取对应文件的InputStream
	 * @param sourceUri 资源uri
	 * @return InputStream
	 * @throws Exception Exception
	 */
	public InputStream getInputStream(String sourceUri) throws Exception;
	
	/**
	 * Excel类型  xls or xlsx
	 * @return  xls or xlsx
	 */
	public String getExcelType();
	
	/**
	 * 过滤需要处理的sheet页
	 * @param sheetIndex sheetIndex
	 * @param sheetName sheetName
	 * @return boolean
	 */
	public boolean sheetFilter(int sheetIndex, String sheetName);
}
