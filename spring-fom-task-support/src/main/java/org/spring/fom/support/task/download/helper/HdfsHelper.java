package org.spring.fom.support.task.download.helper;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

import org.apache.hadoop.fs.Path;
import org.spring.fom.support.task.download.helper.util.HdfsUtil;
import org.spring.fom.support.task.parse.ZipUtil;

/**
 * hdfs的一些默认实现
 * 
 * @author shanhm1991@163.com
 *
 */
public class HdfsHelper implements DownloadHelper, DownloadZipHelper {
	
	private static final int SUCCESS = 200;
	
	private static final int ERROR = 500;
	
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
		return HdfsUtil.delete(masterUrl, slaveUrl, new Path(url)) ? SUCCESS : ERROR;
	}

	@Override
	public String getSourceName(String sourceUri) {
		return new File(sourceUri).getName();
	}

	@Override
	public long zipEntry(String name, String uri, ZipOutputStream zipOutStream) throws Exception {
		return ZipUtil.zipEntry(name, open(uri), zipOutStream);
	}

}
