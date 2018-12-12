package com.fom.context;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm1991
 *
 */
public class DownloaderHdfsConfig extends DownloaderConfig implements IHdfsConfig {
	
	private String hdfs1_url;
	
	private String hdfs2_url;
	
	FileSystem fs;
	
	String signalFile;
	

	protected DownloaderHdfsConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		hdfs1_url = XmlUtil.getString(element, "hdfs1.url", "");
		hdfs2_url = XmlUtil.getString(element, "hdfs2.url", "");
		signalFile = XmlUtil.getString(element, "signal.file", "");
		Configuration conf = new Configuration();
		conf.set("dfs.nameservices", "ngpcluster");//TODO
		conf.set("dfs.ha.namenodes.ngpcluster", "nn1,nn2");
		conf.set("dfs.namenode.rpc-address.ngpcluster.nn1", hdfs1_url);
		conf.set("dfs.namenode.rpc-address.ngpcluster.nn2", hdfs2_url);
		conf.set("dfs.client.failover.proxy.provider.ngpcluster",
				"org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
		conf.set("fs.defaultFS", "hdfs://ngpcluster");
		fs = FileSystem.get(conf);
	}

	@Override
	public final FileSystem getFs() {
		return fs;
	}

	@Override
	public final String getSignalFile() {
		return signalFile;
	}


}
