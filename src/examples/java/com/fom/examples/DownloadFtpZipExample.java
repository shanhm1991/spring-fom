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
@FomContext(remark="下载Ftp服务指定文件列表并打包")
public class DownloadFtpZipExample extends Context {

	private static final long serialVersionUID = 8847859520754630989L;

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
