package com.fom.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fh.search.storage.client.StorageClient;
import com.fh.search.storage.client.StorageClient.ReturnFlag;
import com.fh.search.storage.client.StorageEntity;
import com.fom.util.log.LoggerFactory;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
public class StorageUtil {

	private static final Logger LOG = LoggerFactory.getLogger("storage");
	
	//StorageClient非线程安全，且没有提供close方法,这里保存所有创建成功过的storageClient以便复用
	private static final ConcurrentMap<String, Queue<StorageClient>> storageMap = new ConcurrentHashMap<>(); 

	public static final String storageFile(File file, String zkAddress, boolean deleteOnComplete) throws Exception {
		Queue<StorageClient> queue = storageMap.get(zkAddress);
		if(queue ==null){
			Queue<StorageClient> newQueue = new ConcurrentLinkedQueue<StorageClient>(); 
			queue = storageMap.putIfAbsent(zkAddress, newQueue);
			if(queue == null){
				queue = newQueue;
			}
		}

		StorageClient client = queue.poll();
		if(client == null){
			client = new StorageClient();
			client.setClusterManagerConnectionString(zkAddress);
			client.setReturnFlag(ReturnFlag.RETURN_URL);
			client.setMaxPackageCachedTime(1);
			client.init(); 
		}

		String url = null;
		try{
			StorageEntity entity = new StorageEntity();
			entity.setFileName(file.getName());
			entity.setContent(IOUtils.toByteArray(new FileInputStream(file)));
			url = client.sendFile(entity);
		}finally{
			queue.offer(client);
		}

		LOG.info("上传文件[" + file.getName() + "],url=" + url);
		if(deleteOnComplete && !file.delete()){
			LOG.warn("删除文件失败:" + file.getName()); 
		}
		return url;
	}
	
}
