package example.fom.fomschedul;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.task.updownload.UploadTask;
import org.eto.fom.task.updownload.helper.UploadHelper;
import org.eto.fom.task.updownload.helper.impl.HdfsHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="上传文件到Hdfs服务", stopWithNoCron=true)
public class UploadHdfsExample extends Context {

	private String masterUrl;

	private String slaveUrl;

	private String destPath;

	@SuppressWarnings("unchecked")
	@Override
	protected Collection<UploadTask> scheduleBatch() throws Exception { 
		String path = SpringContext.getPath("/source") + File.separator + "hdfs.jpg";
		UploadHelper helper = new HdfsHelper(masterUrl, slaveUrl);

		Set<UploadTask> set = new HashSet<>();
		set.add(new UploadTask(path, destPath, false, helper));
		return set;

	}

}
