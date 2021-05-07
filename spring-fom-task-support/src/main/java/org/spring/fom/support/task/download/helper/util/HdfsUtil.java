package org.spring.fom.support.task.download.helper.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class HdfsUtil {

	private static Map<String, FileSystem> fsMap = new ConcurrentHashMap<>();

	private static FileSystem get(String masterUrl, String slaveUrl) throws IOException{
		FileSystem fs = fsMap.get(masterUrl + "-" + slaveUrl);
		if(fs != null){
			return fs;
		}

		fs = fsMap.get(slaveUrl + "-" + masterUrl);
		if(fs != null){
			return fs;
		}

		fs = getFileSystem(masterUrl, slaveUrl);
		fsMap.put(masterUrl + "-" + slaveUrl, fs);
		return fs;
	}

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
	 * @param masterUrl 主节点的ip:port
	 * @param slaveUrl  副节点的ip:port
	 * @param isDelSrc isDelSrc
	 * @param src srcPath
	 * @param dest destPath
	 * @throws Exception Exception
	 */
	public static void download(String masterUrl, String slaveUrl, 
			boolean isDelSrc, Path src, Path dest) throws Exception{
		FileSystem fs = get(masterUrl, slaveUrl);
		fs.copyToLocalFile(isDelSrc, src, dest, true); 
	}

	/**
	 * 获取hdfs服务文件流
	 * @param masterUrl 主节点的ip:port
	 * @param slaveUrl  副节点的ip:port
	 * @param path path
	 * @return InputStream
	 * @throws Exception Exception 
	 */
	public static InputStream open(String masterUrl, String slaveUrl, Path path) throws Exception {
		FileSystem fs = get(masterUrl, slaveUrl);
		return fs.open(path);
	}

	/**
	 * 列举目录下文件路径
	 * @param masterUrl 主节点的ip:port
	 * @param slaveUrl  副节点的ip:port
	 * @param path path
	 * @param filter PathFilter
	 * @return list 
	 * @throws Exception Exception
	 */
	public static List<String> list(String masterUrl, String slaveUrl, Path path, PathFilter filter) throws Exception {
		FileSystem fs = get(masterUrl, slaveUrl);
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
	 * 删除指定文件
	 * @param masterUrl 主节点的ip:port
	 * @param slaveUrl  副节点的ip:port
	 * @param path path
	 * @return boolean
	 * @throws Exception Exception
	 */
	public static boolean delete(String masterUrl, String slaveUrl, Path path) throws Exception {
		FileSystem fs = get(masterUrl, slaveUrl);
		return fs.delete(path, true);
	}
	
	/**
	 * 上传文件
	 * @param masterUrl masterUrl
	 * @param slaveUrl slaveUrl
	 * @param localFile localFile
	 * @param remotePath remotePath
	 * @throws Exception  Exception
	 */ 
	public static void upload(String masterUrl, String slaveUrl, File localFile, Path remotePath) throws Exception {
		FileSystem fs = get(masterUrl, slaveUrl);
        fs.copyFromLocalFile(new Path(localFile.getPath()), remotePath);
	}
}
