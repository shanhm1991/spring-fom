package com.fom.defaulter;

import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.helpers.OptionConverter;

import com.fom.context.RuntimeConfig;
import com.fom.context.ContextManager;
import com.fom.util.HdfsUtil;

/**
 * src.path 源文件目录
 * hdfs.master 集群主节点ip:port<br>
 * hdfs.slave 集群副节点ip:port<br>
 * hdfs.signalFile 标记文件<br>
 * downloader.isSrcDel 是否删除源文件<br>
 * downloader.isWithTemp 是否先下载到临时目录<br>
 * downloader.desPath 下载目的目录<br>
 * downloader.zip.entryMax 打包zip的最大文件数<br>
 * downloader.zip.sizeMax 打包zip的最大字节数<br>
 * 
 * @author shanhm
 *
 */
public class HdfsZipDownloadConfig extends RuntimeConfig {

	private FileSystem fs;
	
	private String srcPath;

	private String signalFileName; 

	private String destPath; 

	private boolean isWithTemp; 

	private boolean isDelSrc; 
	
	private int entryMax;
	
	private long sizeMax;

	protected HdfsZipDownloadConfig(String name) {
		super(name);
	}
	
	@Override
	protected void loadExtends() throws Exception {
		String hdfsMaster = load("hdfs.master", "");
		String hdfsSlave = load("hdfs.slave", "");
		fs = HdfsUtil.getFileSystem(hdfsMaster, hdfsSlave);
		
		srcPath = load("src.path", "");
		signalFileName = load("hdfs.signalFile", "");
		isDelSrc = load("downloader.isSrcDel", false);
		isWithTemp = load("downloader.isWithTemp", false);
		destPath = ContextManager.getEnvStr(load("downloader.desPath", ""));
		entryMax = load("downloader.zip.entryMax", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
		sizeMax = OptionConverter.toFileSize(load("downloader.zip.sizeMax", "1GB"), 1024*1024*1024L);
	}

	public FileSystem getFs() {
		return fs;
	}
	
	public String getSrcPath() {
		return srcPath;
	}

	public String getSignalFileName() {
		return signalFileName;
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

	public int getEntryMax() {
		return entryMax;
	}

	public long getSizeMax() {
		return sizeMax;
	}

}
