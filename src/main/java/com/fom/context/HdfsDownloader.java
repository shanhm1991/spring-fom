package com.fom.context;

import com.fom.context.config.HdfsDownloaderConfig;
import com.fom.context.executor.Downloader;
import com.fom.context.executor.Executor;
import com.fom.context.helper.HdfsHelper;

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
	protected void exec(E config) throws Exception {
		HdfsHelper helper = new HdfsHelper(config.getFs(), config.isDelSrc());
		Executor executor = new Downloader(name, srcPath, config.getDestPath(), srcName, true, true, helper);
		executor.exec();
	}
	
}
