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
import com.fom.context.helper.impl.FtpHelper;

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
	protected List<String> getUriList() throws Exception {
		String path = ContextUtil.getContextPath("/source");
		List<String> list = new ArrayList<String>();
		list.add(path + File.separator + "ftp.jpg");
		return list;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		UploaderHelper helper = new FtpHelper(hostname, port, user, passwd);
		String destUri = "http://localhost:4040/fom/";
		return new Uploader(sourceUri, destUri, false, helper);
	}

}
