package com.fom.defaulter;

import java.io.File;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.Downloader;
import com.fom.context.helper.HdfsDownloaderHelper;
import com.fom.util.ScanUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(names="hdfsDownload", remark="扫描下载Hdfs指定目录下文件的默认实现")
public final class HdfsDownload extends Context<HdfsDownloadConfig> {
	

	@Override
	protected List<String> getUriList(HdfsDownloadConfig config) throws Exception { 
		return ScanUtil.scan(config.getFs(), config.getSrcPath(), config.getPattern(), config.getSignalFileName());
	}

	@Override
	protected Executor createExecutor(String sourceUri, HdfsDownloadConfig config) {
		HdfsDownloaderHelper helper = new HdfsDownloaderHelper(config.getFs());
		String sourceName = new File(sourceUri).getName();
		Downloader downloader = new Downloader(getName(), sourceName, sourceUri, config.getDestPath(), 
				config.isDelSrc(), config.isWithTemp(), helper);
		return downloader;
	}
}
