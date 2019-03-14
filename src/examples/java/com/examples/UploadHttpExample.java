package com.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.FomContext;
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
	protected Set<UploadTask> scheduleBatchTasks() throws Exception { 
		String path = ContextUtil.getContextPath("/source") + File.separator + "http.jpg";
		Set<UploadTask> set = new HashSet<>();
		set.add(new UploadTask(path, "http://localhost:4040/fom/", false, new HttpHelper()));
		return set;
	}

}
