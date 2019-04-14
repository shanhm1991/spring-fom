package com.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.FomContext;
import com.fom.task.UploadTask;
import com.fom.task.helper.UploadHelper;
import com.fom.task.helper.impl.FtpHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="上传文件到Ftp服务", stopWithNoCron=true)
public class UploadFtpExample extends Context {

	private static final long serialVersionUID = 3766945707309190003L;
	
	private String hostname;

	private int port;

	private String user;

	private String passwd;
	
	public UploadFtpExample(){
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Set<UploadTask> scheduleBatchTasks() throws Exception {
		String path = ContextUtil.getContextPath("/source")  + File.separator + "ftp.jpg";
		UploadHelper helper = new FtpHelper(hostname, port, user, passwd);
		
		Set<UploadTask> set = new HashSet<>();
		set.add(new UploadTask(path, "http://localhost:4040/fom/", false, helper));
		return set;
	}

}
