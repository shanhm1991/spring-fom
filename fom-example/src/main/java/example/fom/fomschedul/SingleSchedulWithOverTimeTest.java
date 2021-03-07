package example.fom.fomschedul;

import org.eto.fom.context.annotation.FomSchedul;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(remark = "定时单个任务测试，并且设置超时", threadOverTime = 3, cancellable = true)
public class SingleSchedulWithOverTimeTest {
	
	//当任务超时并且cancellable=true，将会收到中断请求，当然具体怎么处理中断由任务实现者决定
	@Scheduled(fixedDelay = 7)
	public long test() { 
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) { 
			return 0;
		}
		return System.currentTimeMillis();
	}
}
