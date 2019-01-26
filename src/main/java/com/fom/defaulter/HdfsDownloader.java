package com.fom.defaulter;

import java.io.File;

import com.fom.context.Context;
import com.fom.context.executor.Downloader;
import com.fom.context.executor.helper.HdfsDownloaderHelper;

/**
 * 
 * @author shanhm
 * 
 * 
 */
public class HdfsDownloader<E extends HdfsDownloaderConfig> extends Context<E> {
	
	protected HdfsDownloader(String name, String path) {
		super(name, path); 
	}
	
	@Override
	protected final void exec(E config) throws Exception {
		HdfsDownloaderHelper helper = new HdfsDownloaderHelper(config.getFs());
		
		String sourceName = new File(sourceUri).getName();
		Downloader downloader = new Downloader(name, sourceName, sourceUri, config.getDestPath(), 
				config.isDelSrc(), config.isWithTemp(), helper);
		downloader.call();
	}
	
}
