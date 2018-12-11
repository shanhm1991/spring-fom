package com.fom.context;


/**
 * 
 * @author shanhm1991
 *
 */
public class HdfsZipDownloaderConfig extends HdfsDownloaderConfig {
	
	int zipContent;
	
	boolean zipIndexContinue;

	protected HdfsZipDownloaderConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		
		
	}

}
