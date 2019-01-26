package com.fom.context.helper;

import java.io.File;
import java.io.InputStream;

import org.apache.hadoop.fs.FileSystem;

import com.fom.util.HdfsUtil;

/**
 * 
 * @author shanhm
 *
 */
public class HdfsDownloaderHelper implements DownloaderHelper {
	
	protected final FileSystem fs;
	
	public HdfsDownloaderHelper(FileSystem fs){
		this.fs = fs;
	}

	@Override
	public final InputStream open(String url) throws Exception {
		return HdfsUtil.open(fs, url);
	}

	@Override
	public final void download(String url, File file) throws Exception {
		HdfsUtil.download(fs, false, url, file.getPath());
	}
 
	@Override
	public final boolean delete(String url) throws Exception {
		return HdfsUtil.delete(fs, url);
	}

}
