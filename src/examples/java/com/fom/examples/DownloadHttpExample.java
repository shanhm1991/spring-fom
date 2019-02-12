package com.fom.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.Downloader;
import com.fom.context.helper.impl.HttpHelper;

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
	protected List<String> getUriList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("http://localhost:4040/fom/index.html");
		return list;
	}
 
	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		String destName = new File(sourceUri).getName();
		return new Downloader(destName, sourceUri, dest, false, true, new HttpHelper());
	}

}
