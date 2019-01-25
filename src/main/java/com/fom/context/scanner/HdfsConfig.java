package com.fom.context.scanner;

import org.apache.hadoop.fs.FileSystem;

import com.fom.context.IConfig;

/**
 * 
 * @author shanhm
 *
 */
public interface HdfsConfig extends IConfig {

	FileSystem getFs();

	String getSignalFileName();

}
