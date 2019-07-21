package com.examples.task.download;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eto.fom.context.Context;
import org.eto.fom.context.FomContext;
import org.eto.fom.task.updownload.DownloadTask;
import org.eto.fom.task.updownload.helper.impl.HttpHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="下载Http指定url", cron="0/15 * * * * ?")
public class DownloadHttpExample extends Context {

	private static final long serialVersionUID = 5448826206898051644L;
	
	private String dest; 
	
	public DownloadHttpExample(){
		dest = new File("").getAbsolutePath() 
				+ File.separator + "download" + File.separator + name;
	}
 
	@SuppressWarnings("unchecked")
	@Override
	protected Set<DownloadTask> scheduleBatchTasks() throws Exception {
		String uri = "http://localhost:4040/fom/index.html";
		String destName = new File(uri).getName();
		
		Set<DownloadTask> set = new HashSet<>();
		set.add(new DownloadTask(uri, destName, dest, false, true, new HttpHelper()));
		return set;
	}

}
