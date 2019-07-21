package com.examples.task.download;

import org.eto.fom.context.Context;
import org.eto.fom.context.FomContext;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="下载Ftp服务指定文件列表并打包", stopWithNoCron=true)
public class DownloadFtpZipExample extends Context {

	private static final long serialVersionUID = 8847859520754630989L;
	
//	private String hostname;
//
//	private int port;
//
//	private String user;
//
//	private String passwd;
//	
//	private String dest;
//	
//	public DownloadFtpZipExample() {
//		 dest = new File("").getAbsolutePath() + "/download/" + name;
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	protected Set<DownloadZipTask> scheduleBatchTasks() throws Exception {
//		List<String> list = new ArrayList<String>();
//		list.add("/ftp/test1.txt");
//		list.add("/ftp/test2.txt");
//		list.add("/ftp/test3.txt");
//		list.add("/ftp/test4.txt");
//		list.add("/ftp/test5.txt");
//		list.add("/ftp/test6.txt");
//		list.add("/ftp/test7.txt");
//		list.add("/ftp/test8.txt");
//		DownloadZipHelper helper = new FtpHelper(hostname, port, user, passwd);
//		
//		Set<DownloadZipTask> tasks = new HashSet<>();
//		tasks.add(new DownloadZipTask(list, "ftpTest", dest, 10, 1024 * 1024, false, helper));
//		return tasks;
//	}

}
