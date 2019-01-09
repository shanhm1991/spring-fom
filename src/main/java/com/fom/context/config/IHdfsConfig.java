package com.fom.context.config;

import org.apache.hadoop.fs.FileSystem;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public interface IHdfsConfig extends IConfig {

	FileSystem getFs();

	String getSignalFile();

}
