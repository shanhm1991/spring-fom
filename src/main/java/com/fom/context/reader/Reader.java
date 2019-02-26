package com.fom.context.reader;

import java.io.Closeable;
import java.util.List;

/**
 * 
 * 为ParseTask以及ZipParseTask定制的一个读取适配器
 * 
 * @author shanhm
 *
 */
public interface Reader extends Closeable {
	
	/**
	 * 读取下一行
	 * @return 行字段内容
	 * @throws Exception Exception
	 */
	List<String> readLine() throws Exception;
	
}
