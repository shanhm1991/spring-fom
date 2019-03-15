package com.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.FomContext;
import com.fom.task.DownloadTask;
import com.fom.task.helper.DownloadHelper;
import com.fom.task.helper.impl.FtpHelper;

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
		config.put("hostname", "");
		config.put("port", "");
		config.put("user", "");
		config.put("passwd", "");
		config.put("destpath", dest);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Set<DownloadTask> scheduleBatchTasks() throws Exception { 
		String hostname = config.get("hostname");
		int port = config.getInt("port", 0);
		String user = config.get("user");
		String passwd = config.get("passwd");
		String dest = config.get("dest");
		
		DownloadHelper helper = new FtpHelper(hostname, port, user, passwd);
		String uri = "/ftp/test.txt";
		String sourceName = new File(uri).getName();
		
		Set<DownloadTask> set = new HashSet<>();
		set.add(new DownloadTask(uri, sourceName, dest, false, true, helper));
		return set;
	}

}
