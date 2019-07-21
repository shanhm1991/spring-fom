package com.examples.task.upload;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eto.fom.boot.ServletUtil;
import org.eto.fom.context.Context;
import org.eto.fom.context.FomContext;
import org.eto.fom.task.updownload.UploadTask;
import org.eto.fom.task.updownload.helper.UploadHelper;
import org.eto.fom.task.updownload.helper.impl.HdfsHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="上传文件到Hdfs服务", stopWithNoCron=true)
public class UploadHdfsExample extends Context {

	private static final long serialVersionUID = -8054944958625990317L;

	private String masterUrl;

	private String slaveUrl;

	private String destPath;

	@SuppressWarnings("unchecked")
	@Override
	protected Set<UploadTask> scheduleBatchTasks() throws Exception { 
		String path = ServletUtil.getContextPath("/source") + File.separator + "hdfs.jpg";
		UploadHelper helper = new HdfsHelper(masterUrl, slaveUrl);

		Set<UploadTask> set = new HashSet<>();
		set.add(new UploadTask(path, destPath, false, helper));
		return set;

	}

}
