package com.fom.examples;

import org.apache.hadoop.fs.FileSystem;

import com.fom.context.Result;
import com.fom.context.ResultHandler;
import com.fom.util.HdfsUtil;

public class DownloadHdfsZipExampleResultHandler implements ResultHandler {
	
	private FileSystem fs;
	
	private String sourceUri;
	
	private boolean isDelSrc;
	
	public DownloadHdfsZipExampleResultHandler(FileSystem fs, String sourceUri, boolean isDelSrc){
		this.fs = fs;
		this.sourceUri = sourceUri;
		this.isDelSrc = isDelSrc;
	}

	@Override
	public void handle(Result result) throws Exception {
		if(!result.isSuccess() || !isDelSrc){
			return;
		}
		HdfsUtil.delete(fs, sourceUri);
	}

}
