package com.fom.context;


/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
public class HdfsZipDownloaderConfig extends HdfsDownloaderConfig {
	
	int zipContent;
	
	protected HdfsZipDownloaderConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		
	}

	public int getZipContent() {
		return zipContent;
	}

	
}
