package example.fom.fomschedul;

import java.io.File;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomConfig;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.task.updownload.helper.UploadHelper;
import org.eto.fom.task.updownload.helper.impl.HdfsHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(cron = "0 0 22 * * ?", remark = "上传文件到hdfs服务")
public class UploadHdfsSchedul {

	@FomConfig("${hdfs.masterUrl}")
	private String masterUrl;

	@FomConfig("${hdfs.slaveUrl}")
	private String slaveUrl;

	@FomConfig("${hdfs.destPath}")
	private String destPath;

	public void upload() throws Exception { 
		UploadHelper helper = new HdfsHelper(masterUrl, slaveUrl);
		String source = SpringContext.getPath("/source") + File.separator + "hdfs.jpg";
		helper.upload(new File(source), destPath);
	}

}
