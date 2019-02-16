package com.fom.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Task;
import com.fom.context.FomContext;
import com.fom.context.helper.UploadHelper;
import com.fom.context.helper.impl.HdfsHelper;
import com.fom.context.task.UploadTask;

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
	protected List<String> getTaskIdList() throws Exception {
		String path = ContextUtil.getContextPath("/source");
		List<String> list = new ArrayList<String>();
		list.add(path + File.separator + "hdfs.jpg");
		return list;
	}

	@Override
	protected Task createTask(String sourceUri) throws Exception {
		Thread.sleep(10000); 
		
		UploadHelper helper = new HdfsHelper(masterUrl, slaveUrl);
		return new UploadTask(sourceUri, destPath, false, helper);
		
	}

}
