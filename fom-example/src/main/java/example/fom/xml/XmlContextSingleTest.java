package example.fom.xml;

import java.text.SimpleDateFormat;

import org.eto.fom.context.core.Context;
import org.eto.fom.context.core.Task;

import example.fom.test.TestTask;

/**
 * 
 * @author shanhm
 *
 */
public class XmlContextSingleTest extends Context<Long> {

	@Override
	public Task<Long> schedul() throws Exception {
		String selfConf = config.get("selfConf");
		log.info("获取自定义配置selfConf = " + selfConf);
		return new TestTask(1);
	}
	
	@Override
	public void onScheduleTerminate(long schedulTimes, long lastTime) {
		String last = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastTime);
		log.info(" 定时任务关闭，共执行{}次任务，最后一次执行时间为{}", schedulTimes, last);
	}
}
