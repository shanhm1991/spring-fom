package com.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.FomContext;
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

	@SuppressWarnings("unchecked")
	@Override
	protected Set<UploadTask> scheduleBatchTasks() throws Exception { 
		String path = ContextUtil.getContextPath("/source") + File.separator + "hdfs.jpg";
		UploadHelper helper = new HdfsHelper(masterUrl, slaveUrl);

		Set<UploadTask> set = new HashSet<>();
		set.add(new UploadTask(path, destPath, false, helper));
		return set;

	}

}
