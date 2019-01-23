package com.fom.defaulter;

import java.io.File;

import org.apache.hadoop.fs.FileSystem;
import org.dom4j.Element;

import com.fom.context.ContextUtil;
import com.fom.context.config.Config;
import com.fom.context.config.IHdfsConfig;
import com.fom.context.executor.IDownloaderConfig;
import com.fom.util.HdfsUtil;
import com.fom.util.XmlUtil;

/**
 * hdfs.master hdfs集群主节点ip:port<br>
 * hdfs.slave hdfs集群副节点ip:port<br>
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HdfsDownloaderConfig extends Config implements IHdfsConfig, IDownloaderConfig {
	
	private String hdfsMaster;
	
	private String hdfsSlave;
	
	private FileSystem fs;
	
	private String signalFileName; 
	
	private String destPath; 
	
	private boolean isWithTemp; 
	
	private boolean isDelSrc; 
	
	protected HdfsDownloaderConfig(String name) {
		super(name);
	}
	
	protected void load(Element e) throws Exception {
		String hdfsMaster = XmlUtil.getString(e, "hdfs.master", "");
		String hdfsSlave = XmlUtil.getString(e, "hdfs.slave", "");
		fs = HdfsUtil.getFileSystem(hdfsMaster, hdfsSlave);
		signalFileName = XmlUtil.getString(e, "hdfs.signalFile", "");
		
		isDelSrc = XmlUtil.getBoolean(e, "downloader.isSrcDel", false);
		isWithTemp = XmlUtil.getBoolean(e, "downloader.isWithTemp", true);
		destPath = ContextUtil.parseEnvStr(XmlUtil.getString(e, "downloader.desPath", ""));
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
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nhdfs.master=" + hdfsMaster);
		builder.append("\nhdfs.slave=" + hdfsSlave);
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof HdfsDownloaderConfig)){
			return false;
		}
		if(o == this){
			return true;
		}
		
		HdfsDownloaderConfig c = (HdfsDownloaderConfig)o; 
		if(!super.equals(c)){
			return false;
		}
		
		return hdfsMaster.equals(c.hdfsMaster)
				&& hdfsSlave.equals(c.hdfsSlave);
	}
}
