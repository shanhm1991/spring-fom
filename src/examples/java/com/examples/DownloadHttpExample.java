package com.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.FomContext;
import com.fom.context.Task;
import com.fom.task.DownloadTask;
import com.fom.task.helper.impl.HttpHelper;

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
 
	@Override
	protected Set<Task> scheduleBatchTasks() throws Exception {
		String uri = "http://localhost:4040/fom/index.html";
		String destName = new File(uri).getName();
		
		Set<Task> set = new HashSet<>();
		set.add(new DownloadTask(uri, destName, dest, false, true, new HttpHelper()));
		return set;
	}

}
