package com.fom.context;

import java.io.File;

import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.helpers.OptionConverter;
import org.dom4j.Element;

import com.fom.context.config.Config;
import com.fom.context.config.IHdfsConfig;
import com.fom.context.executor.IZipDownloaderConfig;
import com.fom.util.HdfsUtil;
import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 * @date 2019年1月23日
 *
 */
public class HdfsZipDownloaderConfig extends Config implements IHdfsConfig, IZipDownloaderConfig {

	private String hdfsMaster;

	private String hdfsSlave;

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

	protected void load(Element e) throws Exception {
		hdfsMaster = XmlUtil.getString(e, "hdfs.master", "");
		hdfsSlave = XmlUtil.getString(e, "hdfs.slave", "");
		fs = HdfsUtil.getFileSystem(hdfsMaster, hdfsSlave);
		signalFileName = XmlUtil.getString(e, "hdfs.signalFile", "");

		isDelSrc = XmlUtil.getBoolean(e, "downloader.isSrcDel", false);
		isWithTemp = XmlUtil.getBoolean(e, "downloader.isWithTemp", true);
		destPath = ContextUtil.parseEnvStr(XmlUtil.getString(e, "downloader.desPath", ""));
		
		entryMax = XmlUtil.getInt(e, "downloader.zip.entryMax", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
		String strSize = XmlUtil.getString(e, "downloader.zip.sizeMax", "1GB");
		sizeMax = OptionConverter.toFileSize(strSize, 1024*1024*1024L);
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
