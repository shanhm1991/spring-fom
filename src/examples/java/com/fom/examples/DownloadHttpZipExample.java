package com.fom.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.FomContext;
import com.fom.context.Task;
import com.fom.task.ZipDownloadTask;
import com.fom.task.helper.impl.HttpHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="下载Http指定url列表并打包", cron="0/30 * * * * ?")
public class DownloadHttpZipExample extends Context {

	private static final long serialVersionUID = 7074729229634949794L;
	
	private String dest; 
	
	public DownloadHttpZipExample(){
		dest = new File("").getAbsolutePath() 
				+ File.separator + "download" + File.separator + name;
	}

	@Override
	protected Set<String> getTaskIdSet() throws Exception {
		Set<String> set = new HashSet<String>();
		set.add("httpTest");
		return set;
	}

	@Override
	protected Task createTask(String taskId) throws Exception {
		Set<String> set = new HashSet<String>();
		set.add("http://localhost:4040/fom/index.html");
		set.add("http://localhost:4040/fom/js/datatables.js");
		set.add("http://localhost:4040/fom/js/datatables.css");
		set.add("http://localhost:4040/fom/js/jquery-3.3.1.js");
		set.add("http://localhost:4040/fom/js/tinybox.js"); 
		set.add("http://localhost:4040/fom/images/details_close.png");
		set.add("http://localhost:4040/fom/images/details_open.png");
		set.add("http://localhost:4040/fom/images/edit.png");
		set.add("http://localhost:4040/fom/images/exec.png");
		set.add("http://localhost:4040/fom/images/load.gif");
		set.add("http://localhost:4040/fom/images/save.png");
		set.add("http://localhost:4040/fom/images/sort_asc_disabled.png");
		set.add("http://localhost:4040/fom/images/sort_asc.png");
		set.add("http://localhost:4040/fom/images/sort_both.png");
		set.add("http://localhost:4040/fom/images/sort_desc_disabled.png");
		set.add("http://localhost:4040/fom/images/sort_desc.png");
		set.add("http://localhost:4040/fom/images/start.png");
		set.add("http://localhost:4040/fom/images/stop.png");
		
		return new ZipDownloadTask(set, taskId, dest, 10, 1024 * 1024, false, new HttpHelper());
	}

}
