package com.fom.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.FomContext;
import com.fom.context.Task;
import com.fom.task.UploadTask;
import com.fom.task.helper.UploadHelper;
import com.fom.task.helper.impl.HdfsHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="上传文件到Hdfs服务")
public class UploadHdfsExample extends Context {

	private static final long serialVersionUID = -8054944958625990317L;
	
	private String masterUrl;

	private String slaveUrl;
	
	private String destPath;

	@Override
	protected Set<String> getTaskIdSet() throws Exception {
		String path = ContextUtil.getContextPath("/source");
		Set<String> set = new HashSet<String>();
		set.add(path + File.separator + "hdfs.jpg");
		return set;
	}

	@Override
	protected Task cronBatchSubmitTask(String taskId) throws Exception { 
		Thread.sleep(10000); 
		
		UploadHelper helper = new HdfsHelper(masterUrl, slaveUrl);
		return new UploadTask(taskId, destPath, false, helper);
		
	}

}
