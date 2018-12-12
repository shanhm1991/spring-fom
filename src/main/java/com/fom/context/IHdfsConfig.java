package com.fom.context;

import org.apache.hadoop.fs.FileSystem;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
interface IHdfsConfig extends IConfig {

	FileSystem getFs();

	String getSignalFile();

}
