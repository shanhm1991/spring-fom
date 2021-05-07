package org.spring.fom.support.task.reader;

import java.util.List;

/**
 * 
 * @author shanhm1991@163.com
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
