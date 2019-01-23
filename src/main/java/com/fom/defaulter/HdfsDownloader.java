package com.fom.defaulter;

import com.fom.context.Context;
import com.fom.context.executor.Downloader;
import com.fom.context.executor.Executor;
import com.fom.context.executor.helper.downloader.HdfsDownloaderHelper;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HdfsDownloader<E extends HdfsDownloaderConfig> extends Context<E> {
	
	protected HdfsDownloader(String name, String path) {
		super(name, path); 
	}

	@Override
	protected final void exec(E config) throws Exception {
		HdfsDownloaderHelper helper = new HdfsDownloaderHelper(config.getFs(), config.isDelSrc());
		Executor executor = new Downloader(name, config, helper);
		executor.exec();
	}
	
}
