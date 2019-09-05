package org.eto.fom.util.file.reader;

import java.util.List;

/**
 * 
 * @author shanhm
 *
 */
public interface IRow {

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
	 * 获取行列数据
	 * @return
	 */
	List<String> getColumnList();
	
}
