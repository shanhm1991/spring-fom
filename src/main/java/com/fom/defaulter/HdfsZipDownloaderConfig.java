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
 * 
 * @author shanhm
 * @date 2019年1月23日
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
	public String getTypeName() {
		return TYPENAME_DOWNLOADER;
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
	public String getDestName() {
		return new File(getUri()).getName();
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

}
