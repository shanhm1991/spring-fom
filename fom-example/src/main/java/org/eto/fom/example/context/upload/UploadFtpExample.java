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
import org.eto.fom.task.updownload.helper.impl.FtpHelper;

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
	protected Collection<UploadTask> scheduleBatch() throws Exception {
		String path = FomListener.getRealPath("/source")  + File.separator + "ftp.jpg";
		UploadHelper helper = new FtpHelper(hostname, port, user, passwd);
		
		Set<UploadTask> set = new HashSet<>();
		set.add(new UploadTask(path, "http://localhost:4040/fom/", false, helper));
		return set;
	}

}
