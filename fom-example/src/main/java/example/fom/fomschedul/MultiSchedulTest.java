package example.fom.fomschedul;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.context.annotation.SchedulCompleter;
import org.eto.fom.context.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(remark = "定时多任务测试", cron = "0/7 * * * * ?", threadOverTime = 4, cancellable = true)
public class MultiSchedulTest implements SchedulCompleter<Object> {
	
	private static final Logger LOG = LoggerFactory.getLogger("MultiSchedulTest");

	@Scheduled
	public long test1(){
		try {
			Thread.sleep(RandomUtils.nextLong(1000, 5000));
		} catch (InterruptedException e) {
			LOG.info("task cancled due to interrupt.");
		} 
		return System.currentTimeMillis();
	}
	
	@Scheduled
	public String test2(){
		try {
			Thread.sleep(RandomUtils.nextLong(1000, 5000));
		} catch (InterruptedException e) {
			LOG.info("task cancled due to interrupt.");
		} 
		return "test2";
	}

	@Override
	public void onScheduleComplete(long schedulTimes, long schedulTime, List<Result<Object>> results) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(schedulTime);
		LOG.info( "第{}次在{}提交的任务全部完成，结果为{}", date, date, results);
	}
}
