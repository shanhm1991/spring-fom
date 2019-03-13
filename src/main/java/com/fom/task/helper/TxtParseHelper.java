package com.fom.task.helper;

import com.fom.task.TxtParseTask;
import com.fom.task.reader.Reader;
/**
 * ParseTask中需要的具体操作方法
 * 
 * @author shanhm
 * 
 * @see TxtParseTask
 *
 */
public interface TxtParseHelper extends ParseHelper {
	
	/**
	 * 获取对应文件的reader
	 * @param sourceUri 资源uri
	 * @return Reader
	 * @throws Exception Exception
	 */
	Reader getReader(String sourceUri) throws Exception;
	
}
