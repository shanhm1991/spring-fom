package example.fom.fomschedul;

import java.io.File;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.task.updownload.helper.impl.HttpHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(remark = "上传文件到http服务")
public class UploadHttpSchedul {
	
	@Value("${http.url:undefined}")
	private String url;

	@Scheduled(cron = "0 0 21 * * ?")
	public void upload() throws Exception { 
		String source = SpringContext.getPath("/source") + File.separator + "http.jpg";
		new HttpHelper().upload(new File(source), url);
	}
}
