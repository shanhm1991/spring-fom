package com.fom.context.executor.reader;

import java.io.Closeable;

/**
 * 
 * @author shanhm
 *
 */
public interface Reader extends Closeable {
	
	String readLine() throws Exception;
	
}
