package com.fom.context;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.fom.util.XmlUtil;

/**
 * <hdfs1.url>
 * <hdfs2.url>
 * <signal.file>
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HdfsZipDownloaderConfig extends ZipDownloaderConfig implements IHdfsConfig {

	private String hdfs_master;

	private String hdfs_slave;

	FileSystem fs;

	String signalFile;

	protected HdfsZipDownloaderConfig(String name) {
		super(name);
	}

	@Override
	void load() throws Exception {
		super.load();
		hdfs_master = XmlUtil.getString(element, "hdfs.master", "");
		hdfs_slave = XmlUtil.getString(element, "hdfs.slave", "");
		Configuration conf = new Configuration();
		conf.set("dfs.nameservices", "proxy");
		conf.set("dfs.ha.namenodes.proxy", "nn1,nn2");
		conf.set("dfs.namenode.rpc-address.proxy.nn1", hdfs_master);
		conf.set("dfs.namenode.rpc-address.proxy.nn2", hdfs_slave);
		conf.set("dfs.client.failover.proxy.provider.proxy",
				"org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
		conf.set("fs.defaultFS", "hdfs://proxy");
		fs = FileSystem.get(conf);
		signalFile = XmlUtil.getString(element, "signal.file", "");
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
	public boolean equals(Object o){
		if(!(o instanceof HdfsZipDownloaderConfig)){
			return false;
		}
		if(o == this){
			return true;
		}

		HdfsZipDownloaderConfig c = (HdfsZipDownloaderConfig)o; 
		if(!super.equals(c)){
			return false;
		}

		return hdfs_master.equals(c.hdfs_master)
				&& hdfs_slave.equals(c.hdfs_slave)
				&& signalFile.equals(c.signalFile);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nhdfs.master=" + hdfs_master);
		builder.append("\nhdfs.slave=" + hdfs_slave);
		builder.append("\nsignal.file=" + signalFile);
		return builder.toString();
	}

	@Override
	public final String getSignalFile() {
		return signalFile;
	}

	@Override
	public FileSystem getFs() {
		return fs;
	}

}
