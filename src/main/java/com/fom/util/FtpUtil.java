package com.fom.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * 
 * @author shanhm
 *
 */
public class FtpUtil {

	private static ConcurrentMap<String, Queue<FTPClient>> clientMap = new ConcurrentHashMap<>(); 
	
	private static String getKey(String hostname, int port, String user, String passwd){
		return hostname + "-" + port + "-u" + user + "-p" + passwd;
	}
	
	private static FTPClient get(String hostname, int port, String user, String passwd) throws Exception{
		String key = getKey(hostname, port, user, passwd);
		Queue<FTPClient> queue = clientMap.get(key);
		if(queue ==null){
			Queue<FTPClient> newQueue = new ConcurrentLinkedQueue<FTPClient>(); 
			queue = clientMap.putIfAbsent(key, newQueue); 
			if(queue == null){
				queue = newQueue;
			}
		}
		
		FTPClient client = queue.poll(); 
		if(client == null){
			client = getFTPClient(hostname, port, user, passwd);
		}
		return client;
	}
	 
	private static void offer(String key, FTPClient client){
		Queue<FTPClient> queue = clientMap.get(key);
		queue.offer(client);
	}

	public static FTPClient getFTPClient(String hostname, int port, String user, String passwd) throws Exception{
		FTPClient ftpClient = new FTPClient();
		ftpClient.setControlEncoding("utf-8");
		ftpClient.connect(hostname, port); 
		ftpClient.login(user, passwd); 
		int replyCode = ftpClient.getReplyCode(); 
		if(!FTPReply.isPositiveCompletion(replyCode)){
			throw new RuntimeException("ftp login failed.");
		}
		return ftpClient;
	}

}
