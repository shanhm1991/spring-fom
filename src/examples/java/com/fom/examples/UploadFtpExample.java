package com.fom.examples;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Task;
import com.fom.context.FomContext;
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
	protected List<String> getTaskIdList() throws Exception {
		String path = ContextUtil.getContextPath("/source");
		List<String> list = new ArrayList<String>();
		list.add(path + File.separator + "ftp.jpg");
		return list;
	}

	@Override
	protected Task createTask(String sourceUri) throws Exception {
		Thread.sleep(5000); 
		
		UploadHelper helper = new FtpHelper(hostname, port, user, passwd);
		String destUri = "http://localhost:4040/fom/";
		return new UploadTask(sourceUri, destUri, false, helper);
	}

}
