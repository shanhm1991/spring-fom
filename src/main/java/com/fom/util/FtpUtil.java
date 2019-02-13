package com.fom.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
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

	private static void offer(String hostname, int port, String user, String passwd, FTPClient client){
		String key = getKey(hostname, port, user, passwd);
		Queue<FTPClient> queue = clientMap.get(key);
		queue.offer(client);
	}

	private static void cd(FTPClient ftpClient,String path) throws Exception { 
		if(!path.startsWith("/")){
			path = "/" + path;
		}
		String currentPath = ftpClient.printWorkingDirectory(); 
		if(currentPath.equals(path)){
			return;
		}

		String[] subDirs = path.split("/");
		subDirs[0] = "/" + subDirs[0];
		ftpClient.changeWorkingDirectory("/");
		for(String subDir : subDirs){
			String strSubDir = new String(subDir.getBytes(ftpClient.getControlEncoding()), "ISO-8859-1");
			ftpClient.makeDirectory(strSubDir);
			if(!ftpClient.sendSiteCommand("chmod 644 " + strSubDir)
					|| ftpClient.changeWorkingDirectory(strSubDir)){
				throw new Exception("cd failed: " + path);
			}
		}
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

		String charset = "GBK";
		if(FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
			charset = "UTF-8";
		}
		ftpClient.setControlEncoding(charset); 
		return ftpClient;
	}

	/**
	 * @param hostname hostname
	 * @param port port
	 * @param user user
	 * @param passwd passwd
	 * @param destPath 上传文件目录(绝对路径)
	 * @param destName 上传文件名称
	 * @param file 本地文件
	 * @throws Exception Exception
	 */
	public static void upload(String hostname, int port, String user, String passwd,
			String destPath, String destName,File file) throws Exception{
		FTPClient ftpClient = null;
		InputStream inputStream = null;
		try{
			inputStream = new FileInputStream(file);
			ftpClient = get(hostname, port, user, passwd);
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			cd(ftpClient, destPath); 
			ftpClient.storeFile(destName, inputStream);
			offer(hostname, port, user, passwd, ftpClient);
		}catch (Exception e) {
			IoUtil.close(ftpClient);
			throw e; 
		}finally{
			IoUtil.close(inputStream); 
		}
	}

	/**
	 * @param hostname
	 * @param port
	 * @param user
	 * @param passwd
	 * @param sourceUri 资源绝对路径
	 * @param file 本地文件
	 * @throws Exception Exception
	 */
	public static void download(String hostname, int port, String user, String passwd,
			String sourceUri, File file) throws Exception{ 
		File remote = new File(sourceUri);
		String path = remote.getParent();
		String name = remote.getName(); 
		FTPClient ftpClient = null;
		OutputStream output = null;
		try{
			output = new FileOutputStream(file); 
			ftpClient = get(hostname, port, user, passwd);
			cd(ftpClient, path);
			ftpClient.retrieveFile(name, output);
			offer(hostname, port, user, passwd, ftpClient);
		}catch (Exception e) {
			IoUtil.close(ftpClient);
			throw e; 
		}finally{
			IoUtil.close(output); 
		}
	}
	
	/**
	 * @param hostname hostname
	 * @param port port
	 * @param user user
	 * @param passwd passwd
	 * @param sourceUri 资源文件绝对路径 
	 * @throws Exception  Exception
	 */
	public static void delete(String hostname, int port, String user, String passwd, String sourceUri) throws Exception{
		File remote = new File(sourceUri);
		String path = remote.getParent();
		String name = remote.getName(); 
		FTPClient ftpClient = null;
		try{
			ftpClient = get(hostname, port, user, passwd); 
			cd(ftpClient, path);
			ftpClient.dele(name); 
			offer(hostname, port, user, passwd, ftpClient);
		}catch (Exception e) {
			IoUtil.close(ftpClient);
			throw e; 
		}
	}

	/**
	 * @param hostname hostname
	 * @param port port
	 * @param user user
	 * @param passwd passwd
	 * @param sourceUri 资源目录绝对路径  
	 * @param filter FTPFileFilter
	 * @return List 
	 * @throws Exception Exception
	 */
	public static List<String> list(String hostname, int port, String user, String passwd
			, String sourceUri, FTPFileFilter filter) throws Exception{
		FTPClient ftpClient = null;
		try{
			ftpClient = get(hostname, port, user, passwd); 
			cd(ftpClient, sourceUri);
			
			FTPFile[] array = null;
			List<String> list = new LinkedList<>();
			if(filter == null){
				array = ftpClient.listFiles();
			}else{
				ftpClient.listFiles(sourceUri, filter);
			}
			if(ArrayUtils.isEmpty(array)){
				return list;
			}
			
			for(FTPFile file : array){
				list.add(sourceUri + File.separator + file.getName());
			}
			offer(hostname, port, user, passwd, ftpClient);
			return list;
		}catch (Exception e) {
			IoUtil.close(ftpClient);
			throw e; 
		}
	}

	/**
	 * @param hostname
	 * @param port
	 * @param user
	 * @param passwd
	 * @param sourceUri 资源绝对路径
	 * @return InputStreamStore
	 * @throws Exception Exception
	 */
	public static InputStreamStore open(String hostname, int port, String user, String passwd, String sourceUri) throws Exception {
		File file = new File(sourceUri);
		String path = file.getParent();
		String name = file.getName();
		FTPClient ftpClient = null;
		try{
			String key = getKey(hostname, port, user, passwd);
			ftpClient = get(hostname, port, user, passwd); 
			cd(ftpClient, path);
			InputStream inputStream = ftpClient.retrieveFileStream(name);
			InputStreamStore store = new InputStreamStore(key, ftpClient, inputStream);
			return store;
		}catch (Exception e) {
			IoUtil.close(ftpClient);
			throw e; 
		}
	}

	public static class InputStreamStore implements Closeable {

		private String key;

		private FTPClient ftpClient;

		private InputStream inputStream;

		private InputStreamStore(String key, FTPClient ftpClient, InputStream inputStream){
			this.key = key;
			this.ftpClient = ftpClient;
			this.inputStream = inputStream;
		}

		public InputStream getInputStream(){
			return inputStream;
		}

		@Override
		public void close() throws IOException {
			IoUtil.close(inputStream); 
			offer(key, ftpClient); 
		}
	}
}
