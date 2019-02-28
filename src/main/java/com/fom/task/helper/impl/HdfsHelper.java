package com.fom.task.helper.impl;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

import org.apache.hadoop.fs.Path;

import com.fom.task.helper.DownloadHelper;
import com.fom.task.helper.UploadHelper;
import com.fom.task.helper.ZipDownloadHelper;
import com.fom.util.HdfsUtil;
import com.fom.util.ZipUtil;

/**
 * hdfs的一些默认实现
 * 
 * @author shanhm
 *
 */
public class HdfsHelper implements DownloadHelper, ZipDownloadHelper, UploadHelper {
	
	protected final String masterUrl;
	
	protected final String slaveUrl;
	
	public HdfsHelper(String masterUrl, String slaveUrl){
		this.masterUrl = masterUrl;
		this.slaveUrl = slaveUrl;
	}

	@Override
	public InputStream open(String url) throws Exception {
		return HdfsUtil.open(masterUrl, slaveUrl, new Path(url));
	}

	@Override
	public void download(String url, File file) throws Exception {
		HdfsUtil.download(masterUrl, slaveUrl, false, new Path(url), new Path(file.getPath()));
	}
 
	@Override
	public int delete(String url) throws Exception {
		return HdfsUtil.delete(masterUrl, slaveUrl, new Path(url)) ? 200 : 500;
	}

	@Override
	public String getSourceName(String sourceUri) {
		return new File(sourceUri).getName();
	}

	@Override
	public int upload(File file, String destUri) throws Exception {
		HdfsUtil.upload(masterUrl, slaveUrl, file, new Path(destUri)); 
		return 200;
	}

	@Override
	public long zipEntry(String name, String uri, ZipOutputStream zipOutStream) throws Exception {
		return ZipUtil.zipEntry(name, open(uri), zipOutStream);
	}

}
