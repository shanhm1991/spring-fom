package com.fom.context.reader;

import java.io.Closeable;

/**
 * 
 * @author shanhm
 *
 */
public interface Reader extends Closeable {
	
	String readLine() throws Exception;
	
}
