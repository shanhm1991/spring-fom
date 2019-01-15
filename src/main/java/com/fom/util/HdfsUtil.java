package com.fom.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
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
	 * 下载hdfs服务文件到本地
	 * @param fs
	 * @param isDelSrc
	 * @param src
	 * @param dest
	 * @throws Exception
	 */
	public static final void downloadAsFile(FileSystem fs, boolean isDelSrc, String src, String dest) throws Exception{
		downloadAsFile(fs, isDelSrc, new Path(src), new Path(dest)); 
	}
	
	/**
	 * 下载hdfs服务文件到本地
	 * @param fs
	 * @param isDelSrc
	 * @param src
	 * @param dest
	 * @throws Exception
	 */
	public static final void downloadAsFile(FileSystem fs, boolean isDelSrc, Path src, Path dest) throws Exception{
		fs.copyToLocalFile(isDelSrc, src, dest, true); 
	}
	
	/**
	 * 获取hdfs服务文件流
	 * @param fs
	 * @param path
	 * @return
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	public static final InputStream open(FileSystem fs, String path) throws Exception {
		return open(fs, new Path(path));
	}
	
	/**
	 * 获取hdfs服务文件流
	 * @param fs
	 * @param path
	 * @return
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	public static final InputStream open(FileSystem fs, Path path) throws Exception {
		return fs.open(path);
	}
	
	/**
	 * 列举目录下文件
	 * @param fs
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static final List<Path> listPath(FileSystem fs, String path) throws Exception {
		return listPath(fs, new Path(path));
	}
	
	/**
	 * 列举目录下文件
	 * @param fs
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static final List<Path> listPath(FileSystem fs, Path path) throws Exception {
		List<Path> list = new LinkedList<>();
		FileStatus[] statusArray = fs.listStatus(path);
		if(ArrayUtils.isEmpty(statusArray)){
			return list;
		}
		for(FileStatus status : statusArray){
			list.add(status.getPath());
		}
		return list;
	}
	
	/**
	 * 列举目录下文件的名称列表
	 * @param fs
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static final List<String> listName(FileSystem fs, String path) throws Exception {
		return listName(fs, new Path(path));
	}
	
	/**
	 * 列举目录下文件的名称列表
	 * @param fs
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static final List<String> listName(FileSystem fs, Path path) throws Exception {
		List<String> list = new LinkedList<>();
		FileStatus[] statusArray = fs.listStatus(path);
		if(ArrayUtils.isEmpty(statusArray)){
			return list;
		}
		for(FileStatus status : statusArray){
			list.add(status.getPath().getName());
		}
		return list;
	}
	
}
