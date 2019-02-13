package com.fom.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.ZipDownloader;
import com.fom.context.helper.impl.HttpHelper;

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
	protected List<String> getUriList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("httpTest");
		return list;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("http://localhost:4040/fom/index.html");
		list.add("http://localhost:4040/fom/js/datatables.js");
		list.add("http://localhost:4040/fom/js/datatables.css");
		list.add("http://localhost:4040/fom/js/jquery-3.3.1.js");
		list.add("http://localhost:4040/fom/js/tinybox.js");
		list.add("http://localhost:4040/fom/images/details_close.png");
		list.add("http://localhost:4040/fom/images/details_open.png");
		list.add("http://localhost:4040/fom/images/edit.png");
		list.add("http://localhost:4040/fom/images/exec.png");
		list.add("http://localhost:4040/fom/images/load.gif");
		list.add("http://localhost:4040/fom/images/save.png");
		list.add("http://localhost:4040/fom/images/sort_asc_disabled.png");
		list.add("http://localhost:4040/fom/images/sort_asc.png");
		list.add("http://localhost:4040/fom/images/sort_both.png");
		list.add("http://localhost:4040/fom/images/sort_desc_disabled.png");
		list.add("http://localhost:4040/fom/images/sort_desc.png");
		list.add("http://localhost:4040/fom/images/start.png");
		list.add("http://localhost:4040/fom/images/stop.png");
		
		return new ZipDownloader("httpTest", list, dest, 
				10, 1024 * 1024, false, new HttpHelper());
	}

}
