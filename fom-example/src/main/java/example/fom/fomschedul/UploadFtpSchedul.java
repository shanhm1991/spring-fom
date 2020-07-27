package example.fom.fomschedul;

import java.io.File;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomConfig;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.task.updownload.helper.UploadHelper;
import org.eto.fom.task.updownload.helper.impl.FtpHelper;

/**
 * 
 * 
 * @author shanhm
 *
 */
@FomSchedul(cron = "0 0 23 * * ?", remark = "上传文件到Ftp服务")
public class UploadFtpSchedul {
	
	@FomConfig("${ftp.hostname}")
	private String hostname;

	@FomConfig("${ftp.port}")
	private int port;

	@FomConfig("${ftp.user}")
	private String user;

	@FomConfig("${ftp.passwd}")
	private String passwd;
	
	public void upload() throws Exception{
		UploadHelper helper = new FtpHelper(hostname, port, user, passwd);
		String source = SpringContext.getPath("/source") + File.separator + "ftp.jpg";
		helper.upload(new File(source), "http://localhost:4040/fom/");
	}
}
