package com.fom.defaulter;

import java.io.File;

import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.helpers.OptionConverter;

import com.fom.context.Config;
import com.fom.context.ContextUtil;
import com.fom.context.executor.ZipDownloaderConfig;
import com.fom.context.scanner.HdfsConfig;
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
public class HdfsZipDownloaderConfig extends Config implements HdfsConfig, ZipDownloaderConfig {

	private FileSystem fs;

	private String signalFileName; 

	private String destPath; 

	private boolean isWithTemp; 

	private boolean isDelSrc; 
	
	private int entryMax;
	
	private long sizeMax;

	protected HdfsZipDownloaderConfig(String name) {
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

	@Override
	public FileSystem getFs() {
		return fs;
	}

	@Override
	public String getSignalFileName() {
		return signalFileName;
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

	@Override
	public int getEntryMax() {
		return entryMax;
	}

	@Override
	public long getSizeMax() {
		return sizeMax;
	}

	/**
	 * new File(uri).getName()
	 */
	@Override
	public String getSourceName(String uri) {
		return new File(uri).getName();
	}


}
