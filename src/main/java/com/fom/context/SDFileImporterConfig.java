package com.fom.context;

import org.apache.hadoop.conf.Configuration;

/**
 * 
 * @author shanhm1991
 *
 */
public class SDFileImporterConfig extends ImporterConfig {
	
	Configuration fsConf;

	protected SDFileImporterConfig(String name) {
		super(name);
		
	}
	
	@Override
	void load() throws Exception {
		fsConf = new Configuration();
		fsConf.set("fs.defaultFS", "file:///");
	}

}
