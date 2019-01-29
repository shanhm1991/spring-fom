package com.fom.examples;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.Downloader;
import com.fom.context.helper.HdfsDownloaderHelper;
import com.fom.util.HdfsUtil;
import com.fom.util.ScanUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="扫描下载Hdfs指定目录下文件的默认实现", cron="0 0/15 * * * ?")
public class DownloadHdfsExample extends Context {

	private FileSystem fs;
	
	private String srcPath = "/test";

	private String destPath = "${webapp.root}/download"; 

	private boolean isWithTemp = true; 

	private boolean isDelSrc = false; 
	
	public DownloadHdfsExample() throws IOException{ 
		String hdfsMaster = "";
		String hdfsSlave = "";
		fs = HdfsUtil.getFileSystem(hdfsMaster, hdfsSlave);
	}

	@Override
	protected List<String> getUriList() throws Exception { 
		return ScanUtil.scan(fs, srcPath, null, null);
	}

	@Override
	protected Executor createExecutor(String sourceUri) {
		HdfsDownloaderHelper helper = new HdfsDownloaderHelper(fs);
		String sourceName = new File(sourceUri).getName();
		Downloader downloader = new Downloader(sourceName, sourceUri, destPath, 
				isDelSrc, isWithTemp, helper);
		return downloader;
	}
}
