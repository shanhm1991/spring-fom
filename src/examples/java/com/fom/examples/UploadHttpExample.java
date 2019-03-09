package com.fom.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.FomContext;
import com.fom.context.Task;
import com.fom.task.UploadTask;
import com.fom.task.helper.impl.HttpHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="上传文件到Http服务")
public class UploadHttpExample extends Context {

	private static final long serialVersionUID = -6676559884214726673L;

	@Override
	protected Set<String> getTaskIdSet() throws Exception {
		String path = ContextUtil.getContextPath("/source");
		Set<String> set = new HashSet<String>();
		set.add(path + File.separator + "http.jpg");
		return set;
	}

	@Override
	protected Task cronBatchSubmitTask(String taskId) throws Exception { 
		Thread.sleep(15000); 
		
		return new UploadTask(taskId, "http://localhost:4040/fom/", false, new HttpHelper());
	}

}
