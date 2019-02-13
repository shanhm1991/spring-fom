package com.fom.context.helper.impl;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

import org.apache.hadoop.fs.Path;

import com.fom.context.helper.DownloaderHelper;
import com.fom.context.helper.UploaderHelper;
import com.fom.context.helper.ZipDownloaderHelper;
import com.fom.util.HdfsUtil;
import com.fom.util.ZipUtil;

/**
 * 
 * @author shanhm
 *
 */
public class HdfsHelper implements DownloaderHelper, ZipDownloaderHelper, UploaderHelper {
	
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long zipEntry(String name, String uri, ZipOutputStream zipOutStream) throws Exception {
		return ZipUtil.zipEntry(name, open(uri), zipOutStream);
	}

}
