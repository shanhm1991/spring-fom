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
import org.apache.hadoop.fs.PathFilter;

/**
 * 
 * @author shanhm
 *
 */
public class HdfsUtil {
	
	/**
	 * 根据集群主副节点的url获取FileSystem
	 * @param masterUrl 主节点的ip:port
	 * @param slaveUrl  副节点的ip:port
	 * @return FileSystem
	 * @throws IOException IOException
	 */
	public static FileSystem getFileSystem(String masterUrl, String slaveUrl) throws IOException{
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
	 * @param fs FileSystem
	 * @param isDelSrc isDelSrc
	 * @param src src
	 * @param dest dest
	 * @throws Exception Exception
	 */
	public static void download(FileSystem fs, boolean isDelSrc, String src, String dest) throws Exception{
		download(fs, isDelSrc, new Path(src), new Path(dest)); 
	}
	
	/**
	 * 下载hdfs服务文件到本地
	 * @param fs FileSystem
	 * @param isDelSrc isDelSrc
	 * @param src srcPath
	 * @param dest destPath
	 * @throws Exception Exception
	 */
	public static void download(FileSystem fs, boolean isDelSrc, Path src, Path dest) throws Exception{
		fs.copyToLocalFile(isDelSrc, src, dest, true); 
	}
	
	/**
	 * 获取hdfs服务文件流
	 * @param fs FileSystem
	 * @param path path
	 * @return InputStream
	 * @throws Exception Exception
	 */
	public static InputStream open(FileSystem fs, String path) throws Exception {
		return open(fs, new Path(path));
	}
	
	/**
	 * 获取hdfs服务文件流
	 * @param fs FileSystem
	 * @param path path
	 * @return InputStream
	 * @throws Exception Exception 
	 */
	public static InputStream open(FileSystem fs, Path path) throws Exception {
		return fs.open(path);
	}
	
	/**
	 * 列举目录下文件路径
	 * @param fs FileSystem
	 * @param path path
	 * @param filter PathFilter
	 * @return List
	 * @throws Exception Exception
	 */
	public static List<String> listPath(FileSystem fs, String path, PathFilter filter) throws Exception {
		return listPath(fs, new Path(path), filter);
	}
	
	/**
	 * 列举目录下文件路径
	 * @param fs FileSystem
	 * @param path path
	 * @param filter PathFilter
	 * @return List
	 * @throws Exception Exception
	 */
	public static List<String> listPath(FileSystem fs, Path path, PathFilter filter) throws Exception {
		List<String> list = new LinkedList<>();
		FileStatus[] statusArray = null;   
		if(filter == null){
			statusArray = fs.listStatus(path);
		}else{
			statusArray = fs.listStatus(path, filter);
		}
		
		if(ArrayUtils.isEmpty(statusArray)){
			return list;
		}
		for(FileStatus status : statusArray){
			list.add(status.getPath().toString());
		}
		return list;
	}
	
	/**
	 * 列举目录下文件路径
	 * @param fs FileSystem
	 * @param path path
	 * @param filter PathFilter
	 * @return List
	 * @throws Exception Exception
	 */
	public static List<String> listName(FileSystem fs, String path, PathFilter filter) throws Exception {
		return listName(fs, new Path(path), filter);
	}
	
	/**
	 * 列举目录下文件路径
	 * @param fs FileSystem
	 * @param path path
	 * @param filter PathFilter
	 * @return List
	 * @throws Exception Exception
	 */
	public static List<String> listName(FileSystem fs, Path path, PathFilter filter) throws Exception {
		List<String> list = new LinkedList<>();
		FileStatus[] statusArray = null; 
		if(filter == null){
			statusArray = fs.listStatus(path);
		}else{
			statusArray = fs.listStatus(path, filter);
		}
		
		if(ArrayUtils.isEmpty(statusArray)){
			return list;
		}
		for(FileStatus status : statusArray){
			list.add(status.getPath().getName());
		}
		return list;
	}
	
	/**
	 * 删除指定文件
	 * @param fs Exception
	 * @param path path
	 * @return boolean
	 * @throws Exception Exception
	 */
	public static boolean delete(FileSystem fs, String path) throws Exception {
		return delete(fs, new Path(path));
	}
	
	/**
	 * 删除指定文件
	 * @param fs FileSystem
	 * @param path path
	 * @return boolean
	 * @throws Exception Exception
	 */
	public static boolean delete(FileSystem fs, Path path) throws Exception {
		return fs.delete(path, true);
	}
}
