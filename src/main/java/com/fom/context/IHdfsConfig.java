package com.fom.context;

import org.apache.hadoop.fs.FileSystem;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
interface IHdfsConfig extends IConfig {

	FileSystem getFs();

	String getSignalFile();

}
