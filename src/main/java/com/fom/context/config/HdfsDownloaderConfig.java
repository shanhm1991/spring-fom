package com.fom.context.config;

import org.apache.hadoop.fs.FileSystem;

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
public class HdfsDownloaderConfig extends DownloaderConfig implements IHdfsConfig {
	
	private String hdfs_master;
	
	private String hdfs_slave;
	
	FileSystem fs;
	
	protected HdfsDownloaderConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		hdfs_master = XmlUtil.getString(element, "hdfs.master", "");
		hdfs_slave = XmlUtil.getString(element, "hdfs.slave", "");
		fs = HdfsUtil.getFileSystem(hdfs_master, hdfs_slave);
	}
	
	@Override
	boolean valid() throws Exception {
		if(!super.valid()){
			return false;
		}
		//...
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nhdfs.master=" + hdfs_master);
		builder.append("\nhdfs.slave=" + hdfs_slave);
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
		
		return hdfs_master.equals(c.hdfs_master)
				&& hdfs_slave.equals(c.hdfs_slave);
	}

	@Override
	public final FileSystem getFs() {
		return fs;
	}

	@Override
	public String getSignalFile() {
		return "";
	}


}
