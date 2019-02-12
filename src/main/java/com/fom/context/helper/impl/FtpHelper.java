package com.fom.context.helper.impl;

import java.io.File;
import java.io.InputStream;

import com.fom.context.helper.DownloaderHelper;
import com.fom.context.helper.UploaderHelper;
import com.fom.context.helper.ZipDownloaderHelper;

/**
 * 
 * @author shanhm
 *
 */
public class FtpHelper implements DownloaderHelper, ZipDownloaderHelper, UploaderHelper {

	@Override
	public InputStream open(String url) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void download(String url, File file) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int delete(String url) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int upload(File file, String destUri) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getSourceName(String sourceUri) {
		// TODO Auto-generated method stub
		return null;
	}

}
