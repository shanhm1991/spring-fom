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
@FomSchedul(remark = "定时多任务测试", cron = "0/7 * * * * ?", threadOverTime = 4, cancellable = true)
public class MultiSchedulTest {

	@Scheduled
	public long test1(){
		String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
		try {
			Thread.sleep(RandomUtils.nextLong(1000, 5000));
		} catch (InterruptedException e) {
			System.out.println(now + " task cancled due to interrupt.");
		} 
		return System.currentTimeMillis();
	}
	
	@Scheduled
	public String test2(){
		String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
		try {
			Thread.sleep(RandomUtils.nextLong(1000, 5000));
		} catch (InterruptedException e) {
			System.out.println(now + " task cancled due to interrupt.");
		} 
		return "test2";
	}
}
