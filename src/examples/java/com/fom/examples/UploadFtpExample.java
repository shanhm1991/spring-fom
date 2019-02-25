package com.fom.examples;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.FomContext;
import com.fom.context.Task;
import com.fom.context.helper.UploadHelper;
import com.fom.context.helper.impl.FtpHelper;
import com.fom.context.task.UploadTask;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="上传文件到Ftp服务")
public class UploadFtpExample extends Context {

	private static final long serialVersionUID = 3766945707309190003L;
	
	private String hostname;

	private int port;

	private String user;

	private String passwd;
	
	public UploadFtpExample(){
		
	}
	
	@Override
	protected Set<String> getTaskIdSet() throws Exception {
		String path = ContextUtil.getContextPath("/source");
		Set<String> set = new HashSet<String>();
		set.add(path + File.separator + "ftp.jpg");
		return set;
	}
 
	@Override
	protected Task createTask(String taskId) throws Exception {
		Thread.sleep(5000); 
		
		UploadHelper helper = new FtpHelper(hostname, port, user, passwd);
		return new UploadTask(taskId, "http://localhost:4040/fom/", false, helper);
	}

}
