package com.fom.context;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.fom.util.XmlUtil;

/**
 * <src.path>
 * <src.pattern>
 * <src.match.fail.del>
 * <scanner.cron>
 * <scanner>
 * <executor>
 * <executor.min>
 * <executor.max>
 * <executor.aliveTime.seconds>
 * <executor.overTime.seconds>
 * <executor.overTime.cancle>
 * <downloader.src.del>
 * <downloader.temp.path>
 * <downloader.dest.path>
 * <hdfs1.url>
 * <hdfs2.url>
 * <signal.file>
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HdfsDownloaderConfig extends DownloaderConfig implements IHdfsConfig {
	
	private String hdfs1_url;
	
	private String hdfs2_url;
	
	FileSystem fs;
	
	String signalFile;
	

	protected HdfsDownloaderConfig(String name) {
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
		builder.append("\nhdfs1.url=" + hdfs1_url);
		builder.append("\nhdfs2.url=" + hdfs2_url);
		builder.append("\nsignal.file=" + signalFile);
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
		
		return hdfs1_url.equals(c.hdfs1_url)
				&& hdfs2_url.equals(c.hdfs2_url)
				&& signalFile.equals(c.signalFile);
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
