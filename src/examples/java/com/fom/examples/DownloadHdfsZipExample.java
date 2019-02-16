package com.fom.examples;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.fom.context.Context;
import com.fom.context.Task;
import com.fom.context.FomContext;
import com.fom.context.helper.impl.HdfsHelper;
import com.fom.context.task.ZipDownloadTask;
import com.fom.util.HdfsUtil;
import com.fom.util.PatternUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="扫描下载Hdfs指定目录下的目录并打包成zip")
public class DownloadHdfsZipExample extends Context {

	private static final long serialVersionUID = -6055805119506513553L;

	private String masterUrl;

	private String slaveUrl;

	private String srPath = "/test";

	private String dest;

	private boolean isDelSrc = false; 

	private int entryMax = 10;

	private long sizeMax = 100 * 1024 * 1024;

	private String signalName;

	public DownloadHdfsZipExample(){
		dest = new File("").getAbsolutePath() + "/download/" + name;
	}

	public DownloadHdfsZipExample(String name){
		super(name);
		dest = new File("").getAbsolutePath() + "/download/" + name;
	}

	@Override
	protected List<String> getTaskIdList() throws Exception {
		Thread.sleep(20000); 
		
		final FileSystem fs = HdfsUtil.getFileSystem(masterUrl, slaveUrl);
		return HdfsUtil.list(masterUrl, slaveUrl, new Path(srPath), new PathFilter(){
			@Override
			public boolean accept(Path path) {
				if(!PatternUtil.match("regex", path.getName())){
					return false;
				}

				FileStatus[] subArray = null;
				try {
					subArray = fs.listStatus(path);
				} catch (Exception e) {
					log.error("", e);
					return false;
				}
				if(ArrayUtils.isEmpty(subArray)){
					return false;
				}
				if(StringUtils.isBlank(signalName)){
					return true;
				}

				for (FileStatus sub : subArray){
					if(signalName.equals(sub.getPath().getName())){
						return true;
					}
				}
				return false;
			}
		});
	}

	@Override
	protected Task createTask(String sourceUri) throws Exception {
		HdfsHelper helper = new HdfsHelper(masterUrl, slaveUrl);
		List<String> pathList = HdfsUtil.list(masterUrl, slaveUrl, new Path(sourceUri), new PathFilter(){
			@Override
			public boolean accept(Path path) {
				if(StringUtils.isBlank(signalName)){
					return true;
				}
				return ! signalName.equals(path.getName());
			}
		});  

		String sourceName = new File(sourceUri).getName();
		DownloadHdfsZipExampleResultHandler handler = 
				new DownloadHdfsZipExampleResultHandler(name, masterUrl, slaveUrl, srPath,isDelSrc);
		return new ZipDownloadTask(pathList, sourceName, dest, entryMax, sizeMax, isDelSrc, helper, handler);
	}

}
