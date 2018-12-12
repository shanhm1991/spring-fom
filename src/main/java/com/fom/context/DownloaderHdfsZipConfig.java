package com.fom.context;


/**
 * 
 * @author shanhm1991
 *
 */
public class DownloaderHdfsZipConfig extends DownloaderHdfsConfig {
	
	int zipContent;
	
	boolean zipIndexContinue;

	protected DownloaderHdfsZipConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		
		
	}

}
