package com.fom.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Task;
import com.fom.context.FomContext;
import com.fom.context.helper.DownloadHelper;
import com.fom.context.helper.impl.FtpHelper;
import com.fom.context.task.DownloadTask;

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
	protected List<String> getTaskIdList() throws Exception {
		Thread.sleep(5000); 
		
		List<String> list = new ArrayList<String>();
		list.add("/ftp/test.txt");
		return list;
	}

	@Override
	protected Task createTask(String sourceUri) throws Exception {
		String hostname = getValue("hostname");
		int port = getInt("port", 0);
		String user = getValue("user");
		String passwd = getValue("passwd");
		String dest = getValue("dest");
		
		DownloadHelper helper = new FtpHelper(hostname, port, user, passwd);
		String sourceName = new File(sourceUri).getName();
		return new DownloadTask(sourceUri, sourceName, dest, false, true, helper);
	}

}
