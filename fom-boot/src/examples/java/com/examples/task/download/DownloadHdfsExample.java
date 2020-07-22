package com.examples.task.download;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.task.updownload.DownloadTask;
import org.eto.fom.task.updownload.helper.DownloadHelper;
import org.eto.fom.task.updownload.helper.impl.HdfsHelper;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.hdfs.HdfsUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="扫描下载Hdfs指定目录下的文件", stopWithNoCron=true)
public class DownloadHdfsExample extends Context {

	private static final long serialVersionUID = -8950649337670940490L;

	private String masterUrl;

	private String slaveUrl;
	
	private String dest;
	
	public DownloadHdfsExample(){
		dest = new File("").getAbsolutePath() + "/download/" + name;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Collection<DownloadTask> scheduleBatch() throws Exception {  
		List<String> list = HdfsUtil.list(masterUrl, slaveUrl, new Path("/test"), new PathFilter(){
			@Override
			public boolean accept(Path path) {
				return PatternUtil.match("regex", path.getName());
			}
		});
		
		Set<DownloadTask> set = new HashSet<>();
		for(String uri : list){
			DownloadHelper helper = new HdfsHelper(masterUrl, slaveUrl);
			String sourceName = new File(uri).getName();
			set.add(new DownloadTask(uri, sourceName, dest, false, true, helper));
		}
		return set;
	}
}
