package com.fom.context;

import org.apache.hadoop.conf.Configuration;

/**
 * 
 * @author shanhm1991
 *
 */
public class SDZipImporterConfig extends ZipImporterConfig {

	Configuration fsConfig;
	
	protected SDZipImporterConfig(String name) {
		super(name);
	}

	@Override
	void load() throws Exception {
		super.load();
		fsConfig = new Configuration();
		fsConfig.set("fs.defaultFS", "file:///");
	}
	
	boolean valid() throws Exception { 
		
		
		
		return super.valid();
	}
}
