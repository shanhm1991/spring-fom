package org.eto.fom.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fh.search.storage.client.StorageClient;
import com.fh.search.storage.client.StorageClient.ReturnFlag;
import com.fh.search.storage.client.StorageEntity;

/**
 * 引入该jar包为了上传文件到文件服务器，但是StorageClient不是线程安全的，所以这里包装下
 * 另外StorageClient在服务地址配置错误时会自己无限重连，导致toomanyfileopens问题，需要注意
 * 
 * @author shanhm
 *
 */
public class StorageUtil {

	private static final Logger LOG = Logger.getLogger(StorageUtil.class);

	//StorageClient非线程安全，且没有提供close方法,这里保存所有创建成功过的storageClient以便复用
	private static ConcurrentMap<String, Queue<StorageClient>> clientMap = new ConcurrentHashMap<>(); 

	private static StorageClient get(String zkAddress) throws Exception{
		Queue<StorageClient> queue = clientMap.get(zkAddress);
		if(queue ==null){
			Queue<StorageClient> newQueue = new ConcurrentLinkedQueue<StorageClient>(); 
			queue = clientMap.putIfAbsent(zkAddress, newQueue);
			if(queue == null){
				queue = newQueue;
			}
		}

		StorageClient client = queue.poll();
		if(client == null){
			client = getStorageClient(zkAddress);
		}
		return client;
	}

	private static void offer(String zkAddress, StorageClient client){
		Queue<StorageClient> queue = clientMap.get(zkAddress);
		queue.offer(client);
	}

	public static StorageClient getStorageClient(String zkAddress) throws Exception {
		StorageClient client = new StorageClient();
		client.setClusterManagerConnectionString(zkAddress);
		client.setReturnFlag(ReturnFlag.RETURN_URL);
		client.setMaxPackageCachedTime(1);
		client.init(); 
		return client;
	}

	public static String storageFile(File file, String zkAddress, boolean deleteOnComplete) throws Exception {
		StorageClient client = get(zkAddress);
		String url = null;
		
		InputStream input = null;
		try{
			input = new FileInputStream(file);
			StorageEntity entity = new StorageEntity();
			entity.setFileName(file.getName());
			entity.setContent(IOUtils.toByteArray(input));
			url = client.sendFile(entity);
		}finally{
			offer(zkAddress, client); 
			IoUtil.close(input);
		}

		if(LOG.isDebugEnabled()){
			LOG.debug("storage file[" + file.getName() + "],url=" + url);
		}
		if(deleteOnComplete && !file.delete()){
			LOG.warn("delete file failed:" + file.getName()); 
		}
		return url;
	}

}
