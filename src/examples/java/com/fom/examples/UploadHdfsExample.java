package com.fom.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.Uploader;
import com.fom.context.helper.UploaderHelper;
import com.fom.context.helper.impl.HdfsHelper;

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
	protected List<String> getUriList() throws Exception {
		String path = ContextUtil.getContextPath("/source");
		List<String> list = new ArrayList<String>();
		list.add(path + File.separator + "hdfs.jpg");
		return list;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		UploaderHelper helper = new HdfsHelper(masterUrl, slaveUrl);
		return new Uploader(sourceUri, destPath, false, helper);
		
	}

}
