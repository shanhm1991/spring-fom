package com.fom.task.reader;

import java.util.List;

/**
 * 
 * @author shanhm
 *
 */
public interface ReaderRow {

	/**
	 * 获取当前行索引
	 * @return
	 */
	int getRowIndex();
	
	/**
	 * 行内容是否为空
	 * @return
	 */
	boolean isEmpty();
	
	/**
	 * 是否是最后一行
	 * @return
	 */
	boolean isLastRow();
	
	/**
	 * 获取行列数据
	 * @return
	 */
	List<String> getColumnList();
	
	/**
	 * 获取Excel的sheet索引
	 * @return
	 */
	int getSheetIndex();
	
	/**
	 * 获取Excel的sheet名称
	 * @return
	 */
	String getSheetName();
	
}
