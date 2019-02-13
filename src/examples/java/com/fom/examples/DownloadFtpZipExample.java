package com.fom.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.ZipDownloader;
import com.fom.context.helper.ZipDownloaderHelper;
import com.fom.context.helper.impl.FtpHelper;

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
	protected List<String> getUriList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("ftpTest");
		return list;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		ZipDownloaderHelper helper = new FtpHelper(hostname, port, user, passwd);
		List<String> list = new ArrayList<String>();
		list.add("/ftp/test1.txt");
		list.add("/ftp/test2.txt");
		list.add("/ftp/test3.txt");
		list.add("/ftp/test4.txt");
		list.add("/ftp/test5.txt");
		list.add("/ftp/test6.txt");
		list.add("/ftp/test7.txt");
		list.add("/ftp/test8.txt");
		
		return new ZipDownloader("httpTest", list, dest, 10, 1024 * 1024, false, helper);
	}

}
