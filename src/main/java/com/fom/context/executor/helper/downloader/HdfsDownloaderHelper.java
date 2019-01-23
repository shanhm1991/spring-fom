package com.fom.context.executor.helper.downloader;

import java.io.File;
import java.io.InputStream;

import org.apache.hadoop.fs.FileSystem;

import com.fom.util.HdfsUtil;

/**
 * 
 * @author shanhm
 * @date 2019年1月22日
 *
 */
public class HdfsDownloaderHelper implements DownloaderHelper {
	
	private final FileSystem fs;
	
	private final boolean isDelSrc;
	
	public HdfsDownloaderHelper(FileSystem fs, boolean isDelSrc){
		this.fs = fs;
		this.isDelSrc = isDelSrc;
	}

	@Override
	public InputStream open(String url) throws Exception {
		return HdfsUtil.open(fs, url);
	}

	@Override
	public void download(String url, File file) throws Exception {
		HdfsUtil.download(fs, isDelSrc, url, file.getPath());
		
	}
 
	@Override
	public boolean delete(String url) throws Exception {
		return HdfsUtil.delete(fs, url);
	}

}
