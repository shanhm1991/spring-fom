package com.fom.defaulter;

import org.apache.hadoop.fs.FileSystem;

import com.fom.context.RuntimeConfig;
import com.fom.context.ContextManager;
import com.fom.util.HdfsUtil;

/**
 * src.path 源文件目录
 * hdfs.master 集群主节点ip:port<br>
 * hdfs.slave 集群副节点ip:port<br>
 * downloader.isSrcDel 是否删除源文件<br>
 * downloader.isWithTemp 是否先下载到临时目录<br>
 * downloader.desPath 下载目的目录<br>
 * 
 * @author shanhm
 *
 */
public class HdfsDownloadConfig extends RuntimeConfig {
	
	private FileSystem fs;
	
	private String srcPath;
	
	private String destPath; 
	
	private boolean isWithTemp; 
	
	private boolean isDelSrc; 
	
	protected HdfsDownloadConfig(String name) {
		super(name);
	}
	
	@Override
	protected void loadExtends() throws Exception {
		String hdfsMaster = load("hdfs.master", "");
		String hdfsSlave = load("hdfs.slave", "");
		fs = HdfsUtil.getFileSystem(hdfsMaster, hdfsSlave);
		srcPath = load("src.path", "");
		isDelSrc = load("downloader.isSrcDel", false);
		isWithTemp = load("downloader.isWithTemp", false);
		destPath = ContextManager.getEnvStr(load("downloader.desPath", ""));
	}
	
	public final FileSystem getFs() {
		return fs;
	}
	
	public String getSrcPath() {
		return srcPath;
	}

	public String getSignalFileName() {
		return "";
	}

	public String getDestPath() {
		return destPath;
	}

	public boolean isWithTemp() {
		return isWithTemp;
	}

	public boolean isDelSrc() {
		return isDelSrc;
	}

}
