package example.fom.fomschedul;

import java.io.File;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.task.updownload.helper.UploadHelper;
import org.eto.fom.task.updownload.helper.impl.HdfsHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(cron = "0 0 22 * * ?", remark = "上传文件到hdfs服务")
public class UploadHdfsSchedul {

	@Value("${hdfs.masterUrl:undefined}")
	private String masterUrl;

	@Value("${hdfs.slaveUrl:undefined}")
	private String slaveUrl;

	@Value("${hdfs.destPath:undefined}")
	private String destPath;

	@Scheduled
	public void upload() throws Exception { 
		UploadHelper helper = new HdfsHelper(masterUrl, slaveUrl);
		String source = SpringContext.getPath("/source") + File.separator + "hdfs.jpg";
		helper.upload(new File(source), destPath);
	}

}
