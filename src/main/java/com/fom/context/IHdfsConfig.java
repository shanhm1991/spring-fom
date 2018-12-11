package com.fom.context;

import org.apache.hadoop.fs.FileSystem;

/**
 * 
 * @author shanhm1991
 *
 */
interface IHdfsConfig extends IConfig {

	FileSystem getFs();

	String getSignalFile();

}
