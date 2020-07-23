package org.eto.fom.example.context.download;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.task.updownload.DownloadTask;
import org.eto.fom.task.updownload.helper.DownloadHelper;
import org.eto.fom.task.updownload.helper.impl.FtpHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="下载Ftp服务指定文件", stopWithNoCron=true)
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
	protected Collection<DownloadTask> scheduleBatch() throws Exception { 
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
