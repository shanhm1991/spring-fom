package com.fom.context.scanner;

import org.apache.hadoop.fs.FileSystem;

import com.fom.context.IConfig;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public interface IHdfsConfig extends IConfig {

	FileSystem getFs();

	String getSignalFileName();

}
