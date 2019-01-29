package com.fom.defaulter;

import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="扫描下载Hdfs指定目录下文件的默认实现")
public final class HdfsDownload extends Context {
	

	@Override
	protected List<String> getUriList() throws Exception { 
//		return ScanUtil.scan(config.getFs(), config.getSrcPath(), config.getPattern(), config.getSignalFileName());
	
		return null;
	}

	@Override
	protected Executor createExecutor(String sourceUri) {
//		HdfsDownloaderHelper helper = new HdfsDownloaderHelper(config.getFs());
//		String sourceName = new File(sourceUri).getName();
//		Downloader downloader = new Downloader(getName(), sourceName, sourceUri, config.getDestPath(), 
//				config.isDelSrc(), config.isWithTemp(), helper);
//		return downloader;
		
		return null;
	}
}
