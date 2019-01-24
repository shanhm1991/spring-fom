package com.fom.defaulter;

import org.apache.hadoop.fs.FileSystem;

import com.fom.context.Config;
import com.fom.context.ContextUtil;
import com.fom.context.executor.DownloaderConfig;
import com.fom.context.scanner.HdfsConfig;
import com.fom.util.HdfsUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HdfsDownloaderConfig extends Config implements HdfsConfig, DownloaderConfig {
	
	private FileSystem fs;
	
	private String signalFileName; 
	
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
		signalFileName = loadExtends("hdfs.signalFile", "");
		
		isDelSrc = loadExtends("downloader.isSrcDel", false);
		isWithTemp = loadExtends("downloader.isWithTemp", false);
		destPath = ContextUtil.getEnvStr(loadExtends("downloader.desPath", ""));
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
	public final FileSystem getFs() {
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

}
