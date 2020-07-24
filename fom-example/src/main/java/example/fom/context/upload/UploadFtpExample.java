package example.fom.context.upload;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eto.fom.context.SpringContext;
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
	
	private String hostname;

	private int port;

	private String user;

	private String passwd;
	
	public UploadFtpExample(){
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Collection<UploadTask> scheduleBatch() throws Exception {
		String path = SpringContext.getPath("/source")  + File.separator + "ftp.jpg";
		UploadHelper helper = new FtpHelper(hostname, port, user, passwd);
		
		Set<UploadTask> set = new HashSet<>();
		set.add(new UploadTask(path, "http://localhost:4040/fom/", false, helper));
		return set;
	}

}
