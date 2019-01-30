package com.fom.examples;

import java.io.File;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.Downloader;
import com.fom.context.helper.HdfsDownloaderHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="扫描下载Hdfs指定目录下文件的默认实现", cron="0/15 * * * * ?")
public class DownloadHdfsExample extends Context {

	private FileSystem fs;
	
	@Override
	protected List<String> getUriList() throws Exception { 
		log.info("没有初始化hdfs环境,不创建下载任务");
		return null;
	}

	@Override
	protected Executor createExecutor(String sourceUri) {
		HdfsDownloaderHelper helper = new HdfsDownloaderHelper(fs);
		String sourceName = new File(sourceUri).getName();
		Downloader downloader = new Downloader(sourceName, sourceUri, "${webapp.root}/download", 
				false, true, helper);
		return downloader;
	}
}
