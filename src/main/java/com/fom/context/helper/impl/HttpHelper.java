package com.fom.context.helper.impl;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

import com.fom.context.helper.DownloaderHelper;
import com.fom.context.helper.UploaderHelper;
import com.fom.context.helper.ZipDownloaderHelper;
import com.fom.util.HttpUtil;
import com.fom.util.ZipUtil;

/**
 * 
 * @author shanhm
 *
 */
public class HttpHelper implements DownloaderHelper, ZipDownloaderHelper, UploaderHelper {

	@Override
	public InputStream open(String url) throws Exception {
		return HttpUtil.open(url);
	}

	@Override
	public void download(String url, File file) throws Exception {
		HttpUtil.download(url, file);
	}

	@Override
	public int delete(String url) throws Exception {
		return HttpUtil.delete(url);
	}

	@Override
	public String getSourceName(String sourceUri) {
		return new File(sourceUri).getName();
	}

	@Override
	public int upload(File file, String destUri) throws Exception {
		return HttpUtil.upload(file, destUri, null);
	}

	@Override
	public long zipEntry(String name, String uri, ZipOutputStream zipOutStream) throws Exception {
		return ZipUtil.zipEntry(name, open(uri), zipOutStream);
	}

}
