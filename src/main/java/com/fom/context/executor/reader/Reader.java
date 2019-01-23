package com.fom.context.executor.reader;

import java.io.Closeable;
import java.io.InputStream;

/**
 * 
 * @author shanhm
 * @date 2019年1月23日
 *
 */
public interface Reader extends Closeable {
	
	void init(String uri);
	
	void init(InputStream in);

	String readLine();
	
}
