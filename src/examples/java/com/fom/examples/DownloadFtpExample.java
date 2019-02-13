package com.fom.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.Downloader;
import com.fom.context.helper.DownloaderHelper;
import com.fom.context.helper.impl.FtpHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="下载Ftp服务指定文件")
public class DownloadFtpExample extends Context {

	private static final long serialVersionUID = 9006928975258471271L;

	public DownloadFtpExample(){
		String dest = new File("").getAbsolutePath() + "/download/" + name;
		setValue("hostname", "");
		setValue("port", "");
		setValue("user", "");
		setValue("passwd", "");
		setValue("destpath", dest);
	}

	@Override
	protected List<String> getUriList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("/ftp/test.txt");
		return list;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		String hostname = getValue("hostname");
		int port = getInt("port", 0);
		String user = getValue("user");
		String passwd = getValue("passwd");
		String dest = getValue("dest");
		
		DownloaderHelper helper = new FtpHelper(hostname, port, user, passwd);
		String sourceName = new File(sourceUri).getName();
		return new Downloader(sourceName, sourceUri, dest, false, true, helper);
	}

}
