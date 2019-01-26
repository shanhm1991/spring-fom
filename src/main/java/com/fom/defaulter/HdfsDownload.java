package com.fom.defaulter;

import java.io.File;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.executor.Downloader;
import com.fom.context.helper.HdfsDownloaderHelper;
import com.fom.util.ScanUtil;

public final class HdfsDownload extends Context<HdfsDownloadConfig> {
	
	protected HdfsDownload(String name) {
		super(name);
	}

	@Override
	protected List<String> scan(String srcUri, HdfsDownloadConfig config) throws Exception { 
		return ScanUtil.scan(config.getFs(), srcUri, config.getPattern(), config.getSignalFileName());
	}

	@Override
	protected Executor createExecutor(String sourceUri, HdfsDownloadConfig config) {
		HdfsDownloaderHelper helper = new HdfsDownloaderHelper(config.getFs());
		String sourceName = new File(sourceUri).getName();
		Downloader downloader = new Downloader(name, sourceName, sourceUri, config.getDestPath(), 
				config.isDelSrc(), config.isWithTemp(), helper);
		return downloader;
	}
}
