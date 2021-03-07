package example.fom.fomschedul;

import org.eto.fom.context.annotation.FomSchedul;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(remark = "定时单个任务测试")
public class SingleScheduleTest {
	
	@Scheduled(cron = "0/5 * * * * ?")
	public long test(){
		return System.currentTimeMillis();
	}
}
