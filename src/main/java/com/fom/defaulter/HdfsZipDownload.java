package com.fom.defaulter;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.ResultHandler;
import com.fom.context.executor.ZipDownloader;
import com.fom.context.helper.HdfsZipDownloaderHelper;
import com.fom.context.helper.ZipDownloaderHelper;
import com.fom.util.HdfsUtil;
import com.fom.util.ScanUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(names="HdfsZipDownload", remark="扫描下载Hdfs指定目录下的目录并打包成zip")
public final class HdfsZipDownload extends Context<HdfsZipDownloadConfig> {

	@Override
	protected List<String> getUriList(HdfsZipDownloadConfig config) throws Exception {
		return ScanUtil.scan(config.getFs(), config.getSrcPath(), config.getPattern(), config.getSignalFileName());
	}

	@Override
	protected Executor createExecutor(String sourceUri, HdfsZipDownloadConfig config) throws Exception {
		HdfsZipDownloaderHelper helper = new HdfsZipDownloaderHelper(config.getFs());
		List<String> pathList = HdfsUtil.listPath(config.getFs(), sourceUri, new PathFilter(){
			@Override
			public boolean accept(Path path) {
				if(StringUtils.isBlank(config.getSignalFileName())){
					return true;
				}
				return ! config.getSignalFileName().equals(path.getName());
			}
		});  
		
		String sourceName = new File(sourceUri).getName();
		Handler handler = new Handler(sourceUri, config.isDelSrc(), helper);
		ZipDownloader zipDownloader = new ZipDownloader(getName(), sourceName, pathList, config.getDestPath(), 
				config.getEntryMax(), config.getSizeMax(), config.isDelSrc(), helper, handler);
		return zipDownloader;
	}
	
	private class Handler implements ResultHandler {
		
		private String sourceUri;
		
		private boolean isDelSrc;
		
		private ZipDownloaderHelper helper;
		
		public Handler(String sourceUri, boolean isDelSrc, ZipDownloaderHelper helper){
			this.sourceUri = sourceUri;
			this.isDelSrc = isDelSrc;
			this.helper = helper;
		}

		@Override
		public void handle(boolean result) throws Exception { 
			if(result && isDelSrc && !helper.delete(sourceUri)) {
				HdfsZipDownload.this.log.error("删除源目录失败."); 
			}
		} 
	}
	
}
