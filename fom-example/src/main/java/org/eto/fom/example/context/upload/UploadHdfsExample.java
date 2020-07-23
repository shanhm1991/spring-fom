package org.eto.fom.example.context.upload;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eto.fom.boot.listener.FomListener;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
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
	protected Collection<UploadTask> scheduleBatch() throws Exception { 
		String path = FomListener.getRealPath("/source") + File.separator + "hdfs.jpg";
		UploadHelper helper = new HdfsHelper(masterUrl, slaveUrl);

		Set<UploadTask> set = new HashSet<>();
		set.add(new UploadTask(path, destPath, false, helper));
		return set;

	}

}
