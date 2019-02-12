package com.fom.examples;

import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="上传文件到Ftp服务")
public class UploadFtpExample extends Context {

	private static final long serialVersionUID = 3766945707309190003L;

	@Override
	protected List<String> getUriList() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
