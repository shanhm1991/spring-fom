package example.fom.fomschedul;

import java.io.File;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomConfig;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.task.updownload.helper.UploadHelper;
import org.eto.fom.task.updownload.helper.impl.FtpHelper;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * 
 * @author shanhm
 *
 */
@FomSchedul(cron = "0 0 23 * * ?", remark = "上传文件到Ftp服务")
public class UploadFtpSchedul {
	
	@FomConfig("${ftp.hostname:undefined}")
	private String hostname;

	@FomConfig("${ftp.port:4000}")
	private int port;

	@FomConfig("${ftp.user:undefined}")
	private String user;

	@FomConfig("${ftp.passwd:undefined}")
	private String passwd;
	
	@Scheduled
	public void upload() throws Exception{
		UploadHelper helper = new FtpHelper(hostname, port, user, passwd);
		String source = SpringContext.getPath("/source") + File.separator + "ftp.jpg";
		helper.upload(new File(source), "http://localhost:4040/fom/");
	}
}
