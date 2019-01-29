package com.fom.defaulter;

import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.ResultHandler;
import com.fom.context.helper.ZipDownloaderHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="扫描下载Hdfs指定目录下的目录并打包成zip")
public final class HdfsZipDownload extends Context {

	@Override
	protected List<String> getUriList() throws Exception {
//		return ScanUtil.scan(config.getFs(), config.getSrcPath(), config.getPattern(), config.getSignalFileName());
	
		return null;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
//		HdfsZipDownloaderHelper helper = new HdfsZipDownloaderHelper(config.getFs());
//		List<String> pathList = HdfsUtil.listPath(config.getFs(), sourceUri, new PathFilter(){
//			@Override
//			public boolean accept(Path path) {
//				if(StringUtils.isBlank(config.getSignalFileName())){
//					return true;
//				}
//				return ! config.getSignalFileName().equals(path.getName());
//			}
//		});  
//		
//		String sourceName = new File(sourceUri).getName();
//		Handler handler = new Handler(sourceUri, config.isDelSrc(), helper);
//		ZipDownloader zipDownloader = new ZipDownloader(getName(), sourceName, pathList, config.getDestPath(), 
//				config.getEntryMax(), config.getSizeMax(), config.isDelSrc(), helper, handler);
//		return zipDownloader;
		return null;
		
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