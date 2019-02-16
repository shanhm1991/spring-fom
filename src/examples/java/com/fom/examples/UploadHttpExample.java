package com.fom.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Task;
import com.fom.context.FomContext;
import com.fom.context.helper.impl.HttpHelper;
import com.fom.context.task.UploadTask;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="上传文件到Http服务")
public class UploadHttpExample extends Context {

	private static final long serialVersionUID = -6676559884214726673L;

	@Override
	protected List<String> getTaskIdList() throws Exception {
		String path = ContextUtil.getContextPath("/source");
		List<String> list = new ArrayList<String>();
		list.add(path + File.separator + "http.jpg");
		return list;
	}

	@Override
	protected Task createTask(String sourceUri) throws Exception {
		Thread.sleep(15000); 
		
		String destUri = "http://localhost:4040/fom/";
		return new UploadTask(sourceUri, destUri, false, new HttpHelper());
	}

}
