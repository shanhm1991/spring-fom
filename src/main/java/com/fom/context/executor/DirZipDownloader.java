package com.fom.context.executor;

import java.util.List;

import com.fom.context.ExceptionHandler;
import com.fom.context.helper.ZipDownloaderHelper;

public class DirZipDownloader extends ZipDownloader {

	public DirZipDownloader(String zipName, List<String> uriList, String destPath, int zipEntryMax, long zipSizeMax,
			boolean isDelSrc, ZipDownloaderHelper helper) {
		super(zipName, uriList, destPath, zipEntryMax, zipSizeMax, isDelSrc, helper);
	}
	
	public DirZipDownloader(String zipName, List<String> uriList, String destPath, int zipEntryMax, long zipSizeMax,
			boolean isDelSrc, ZipDownloaderHelper helper, ExceptionHandler exceptionHandler) {
		super(zipName, uriList, destPath, zipEntryMax, zipSizeMax, isDelSrc, helper, exceptionHandler);
	}
	
	/**
	 * 添加一个删除源目录的动作
	 */
	@Override
	protected boolean onComplete() throws Exception {
		if(super.onComplete()){
			return false;
		}
		if(isDelSrc && !helper.delete(sourceUri)) {
			log.error("删除源目录失败."); 
			return false;
		}
		return true;
	}

}
