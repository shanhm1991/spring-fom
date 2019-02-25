package com.fom.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.FomContext;
import com.fom.context.Task;
import com.fom.context.helper.ZipDownloadHelper;
import com.fom.context.helper.impl.FtpHelper;
import com.fom.context.task.ZipDownloadTask;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="下载Ftp服务指定文件列表并打包")
public class DownloadFtpZipExample extends Context {

	private static final long serialVersionUID = 8847859520754630989L;
	
	private String hostname;

	private int port;

	private String user;

	private String passwd;
	
	private String dest;
	
	public DownloadFtpZipExample() {
		 dest = new File("").getAbsolutePath() + "/download/" + name;
	}

	@Override
	protected Set<String> getTaskIdSet() throws Exception {
		Thread.sleep(10000); 
		
		Set<String> set = new HashSet<String>();
		set.add("ftpTest");
		return set;
	}

	@Override
	protected Task createTask(String taskId) throws Exception {
		ZipDownloadHelper helper = new FtpHelper(hostname, port, user, passwd);
		Set<String> set = new HashSet<String>();
		set.add("/ftp/test1.txt");
		set.add("/ftp/test2.txt");
		set.add("/ftp/test3.txt");
		set.add("/ftp/test4.txt");
		set.add("/ftp/test5.txt");
		set.add("/ftp/test6.txt");
		set.add("/ftp/test7.txt");
		set.add("/ftp/test8.txt");
		
		return new ZipDownloadTask(set, taskId, dest, 10, 1024 * 1024, false, helper);
	}

}
