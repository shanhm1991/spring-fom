package com.fom.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * 
 * @author shanhm
 * @date 2019年1月10日
 *
 */
public class HdfsUtil {
	
	/**
	 * 根据集群主副节点的url获取FileSystem
	 * @param masterUrl 主节点的ip:port
	 * @param slaveUrl  副节点的ip:port
	 * @return
	 * @throws IOException
	 */
	public static final FileSystem getFileSystem(String masterUrl, String slaveUrl) throws IOException{
		Configuration conf = new Configuration();
		conf.set("dfs.nameservices", "proxy");
		conf.set("dfs.ha.namenodes.proxy", "nn1,nn2");
		conf.set("dfs.namenode.rpc-address.proxy.nn1", masterUrl);
		conf.set("dfs.namenode.rpc-address.proxy.nn2", slaveUrl);
		conf.set("dfs.client.failover.proxy.provider.proxy",
				"org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
		conf.set("fs.defaultFS", "hdfs://proxy");
		return FileSystem.get(conf);
	}

	/**
	 * 下载hdfs上的文件到本地
	 * @param fs
	 * @param isDelSrc
	 * @param srcPath
	 * @param destPath
	 * @throws Exception
	 */
	public static final void downloadAsFile(FileSystem fs, boolean isDelSrc, String srcPath, String destPath) throws Exception{
		fs.copyToLocalFile(isDelSrc, new Path(srcPath), new Path(destPath), true); 
	}
	
	/**
	 * 下载hdfs上的文件到本地
	 * @param fs
	 * @param isDelSrc
	 * @param srcPath
	 * @param destPath
	 * @throws Exception
	 */
	public static final void downloadAsFile(FileSystem fs, boolean isDelSrc, Path srcPath, Path destPath) throws Exception{
		fs.copyToLocalFile(isDelSrc, srcPath, destPath, true); 
	}
	
	/**
	 * 获取hdfs的文件流，记得自行关闭
	 * @param fs
	 * @param srcPath
	 * @return
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	public static final InputStream downloadAsStream(FileSystem fs, String srcPath) throws Exception{
		return fs.open(new Path(srcPath));
	}
	
	/**
	 * 获取hdfs的文件流，记得自行关闭
	 * @param fs
	 * @param srcPath
	 * @return
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	public static final InputStream downloadAsStream(FileSystem fs, Path srcPath) throws Exception{
		return fs.open(srcPath);
	}
}
