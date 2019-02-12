package com.fom.context.helper.impl;

import java.io.File;
import java.io.InputStream;

import org.apache.hadoop.fs.FileSystem;

import com.fom.context.helper.DownloaderHelper;
import com.fom.context.helper.ZipDownloaderHelper;
import com.fom.util.HdfsUtil;

/**
 * 
 * @author shanhm
 *
 */
public class HdfsHelper implements DownloaderHelper, ZipDownloaderHelper {
	
	protected final FileSystem fs;
	
	public HdfsHelper(FileSystem fs){
		this.fs = fs;
	}

	@Override
	public InputStream open(String url) throws Exception {
		return HdfsUtil.open(fs, url);
	}

	@Override
	public void download(String url, File file) throws Exception {
		HdfsUtil.download(fs, false, url, file.getPath());
	}
 
	@Override
	public int delete(String url) throws Exception {
		return HdfsUtil.delete(fs, url) ? 200 : 500;
	}

	@Override
	public String getSourceName(String sourceUri) {
		return new File(sourceUri).getName();
	}

}
