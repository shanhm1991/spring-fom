package com.fom.context.executor.reader;

import java.io.Closeable;

/**
 * 
 * @author shanhm
 * @date 2019年1月23日
 *
 */
public interface Reader extends Closeable {
	
	String readLine() throws Exception;
	
}
