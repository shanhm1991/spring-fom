package com.fom.defaulter;

import java.io.File;

import org.apache.hadoop.fs.FileSystem;

import com.fom.context.Config;
import com.fom.context.ContextUtil;
import com.fom.context.executor.DownloaderConfig;
import com.fom.context.scanner.HdfsConfig;
import com.fom.util.HdfsUtil;

/**
 * hdfs.master 集群主节点ip:port<br>
 * hdfs.slave 集群副节点ip:port<br>
 * downloader.isSrcDel 是否删除源文件<br>
 * downloader.isWithTemp 是否先下载到临时目录<br>
 * downloader.desPath 下载目的目录<br>
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HdfsDownloaderConfig extends Config implements HdfsConfig, DownloaderConfig {
	
	private FileSystem fs;
	
	private String destPath; 
	
	private boolean isWithTemp; 
	
	private boolean isDelSrc; 
	
	protected HdfsDownloaderConfig(String name) {
		super(name);
	}
	
	@Override
	protected void loadExtends() throws Exception {
		String hdfsMaster = loadExtends("hdfs.master", "");
		String hdfsSlave = loadExtends("hdfs.slave", "");
		fs = HdfsUtil.getFileSystem(hdfsMaster, hdfsSlave);
		
		isDelSrc = loadExtends("downloader.isSrcDel", false);
		isWithTemp = loadExtends("downloader.isWithTemp", false);
		destPath = ContextUtil.getEnvStr(loadExtends("downloader.desPath", ""));
	}
	
	@Override
	public String getType() {
		return TYPE_DOWNLOADER;
	}
	
	@Override
	public final FileSystem getFs() {
		return fs;
	}

	@Override
	public String getSignalFileName() {
		return "";
	}

	@Override
	public String getDestPath() {
		return destPath;
	}

	@Override
	public boolean isWithTemp() {
		return isWithTemp;
	}

	@Override
	public boolean isDelSrc() {
		return isDelSrc;
	}

	/**
	 * new File(uri).getName()
	 */
	@Override
	public String getSourceName(String uri) {
		return new File(uri).getName();
	}

}
