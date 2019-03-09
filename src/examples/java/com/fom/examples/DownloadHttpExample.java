package com.fom.examples;

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
	protected Set<String> getTaskIdSet() throws Exception {
		Set<String> set = new HashSet<String>();
		set.add("http://localhost:4040/fom/index.html");
		return set;
	}
 
	@Override
	protected Task cronBatchSubmitTask(String taskId) throws Exception {
		String destName = new File(taskId).getName();
		return new DownloadTask(taskId, destName, dest, false, true, new HttpHelper());
	}

}
