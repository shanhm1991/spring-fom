package example.fom.fomschedul;

import java.io.File;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomConfig;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.task.updownload.helper.impl.HttpHelper;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(cron = "0 0 21 * * ?", remark = "上传文件到http服务")
public class UploadHttpSchedul {
	
	@FomConfig("${http.url}")
	private String url;

	protected void upload() throws Exception { 
		String source = SpringContext.getPath("/source") + File.separator + "http.jpg";
		new HttpHelper().upload(new File(source), url);
	}
}
