package com.fom.context.helper.impl;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

import com.fom.context.helper.DownloadHelper;
import com.fom.context.helper.UploadHelper;
import com.fom.context.helper.ZipDownloadHelper;
import com.fom.util.FtpUtil;
import com.fom.util.FtpUtil.InputStreamStore;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 
 * @author shanhm
 *
 */
public class FtpHelper implements DownloadHelper, ZipDownloadHelper, UploadHelper {
	
	private String hostname;
	
	private int port;
	
	private String user;
	
	private String passwd;
	
	public FtpHelper(String hostname, int port, String user, String passwd){
		this.hostname = hostname;
		this.port = port;
		this.user = user;
		this.passwd = passwd;
	}

	@Override
	public InputStream open(String url) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void download(String url, File file) throws Exception {
		FtpUtil.download(hostname, port, user, passwd, url, file);
	}

	@Override
	public int delete(String url) throws Exception {
		FtpUtil.delete(hostname, port, user, passwd, url);
		return 200;
	}

	@Override
	public int upload(File file, String destUri) throws Exception {
		String name = file.getName();
		FtpUtil.upload(hostname, port, user, passwd, destUri, name, file);
		return 200;
	}

	@Override
	public String getSourceName(String sourceUri) {
		return new File(sourceUri).getName();
	}

	@Override
	public long zipEntry(String name, String uri, ZipOutputStream zipOutStream) throws Exception {
		InputStreamStore store = FtpUtil.open(hostname, port, user, passwd, uri);
		try{
			return ZipUtil.zipEntry(name, store.getInputStream(), zipOutStream);
		}finally{
			IoUtil.close(store); 
		}
	}

}
