package com.fom.context.config;

import org.apache.hadoop.fs.FileSystem;

/**
 * 
 * @author shanhm
 * @date 2019年1月10日
 *
 */
public class HdfsUploaderConfig extends UploaderConfig implements IHdfsConfig {

	protected HdfsUploaderConfig(String name) {
		super(name);
	}

	@Override
	public FileSystem getFs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSignalFile() {
		// TODO Auto-generated method stub
		return null;
	}

}
