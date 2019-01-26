package com.fom.defaulter;

import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.helpers.OptionConverter;

import com.fom.context.Config;
import com.fom.context.ContextUtil;
import com.fom.util.HdfsUtil;

/**
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
public class HdfsZipDownloadConfig extends Config {

	private FileSystem fs;

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
		String hdfsMaster = loadExtends("hdfs.master", "");
		String hdfsSlave = loadExtends("hdfs.slave", "");
		fs = HdfsUtil.getFileSystem(hdfsMaster, hdfsSlave);
		signalFileName = loadExtends("hdfs.signalFile", "");
		
		isDelSrc = loadExtends("downloader.isSrcDel", false);
		isWithTemp = loadExtends("downloader.isWithTemp", false);
		destPath = ContextUtil.getEnvStr(loadExtends("downloader.desPath", ""));
		
		entryMax = loadExtends("downloader.zip.entryMax", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
		sizeMax = OptionConverter.toFileSize(loadExtends("downloader.zip.sizeMax", "1GB"), 1024*1024*1024L);
	}

	@Override
	public String getType() {
		return TYPE_DOWNLOADER;
	}

	public FileSystem getFs() {
		return fs;
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
