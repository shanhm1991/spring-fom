package example.fom.fomschedul;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.RandomUtils;
import org.eto.fom.context.annotation.FomSchedul;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(remark = "定时单任务测试")
public class SingleScheduleTest {
	
	@Scheduled(fixedDelay = 7)
	public long test(){
		String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
		long sleep = RandomUtils.nextLong(1000, 5000);
		try {
			Thread.sleep(sleep);
		} catch (InterruptedException e) {
			System.out.println(now + " task cancled due to interrupt.");
			return sleep;
		} 
		return sleep;
	}
}
