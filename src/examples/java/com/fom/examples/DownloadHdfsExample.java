package com.fom.examples;

import java.io.File;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.Downloader;
import com.fom.context.helper.DownloaderHelper;
import com.fom.context.helper.impl.HdfsHelper;
import com.fom.util.HdfsUtil;
import com.fom.util.PatternUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="扫描下载Hdfs指定目录下的文件")
public class DownloadHdfsExample extends Context {

	private static final long serialVersionUID = -8950649337670940490L;

	private String masterUrl;

	private String slaveUrl;
	
	private String dest;
	
	public DownloadHdfsExample(){
		dest = new File("").getAbsolutePath() + "/download/" + name;
	}

	@Override
	protected List<String> getUriList() throws Exception { 
		return HdfsUtil.list(masterUrl, slaveUrl, new Path("/test"), new PathFilter(){
			@Override
			public boolean accept(Path path) {
				return PatternUtil.match("regex", path.getName());
			}
		});
	}

	@Override
	protected Executor createExecutor(String sourceUri) {
		DownloaderHelper helper = new HdfsHelper(masterUrl, slaveUrl);
		String sourceName = new File(sourceUri).getName();
		return new Downloader(sourceName, sourceUri, dest, false, true, helper);
	}
}
