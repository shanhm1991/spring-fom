package example.fom.fomschedul;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.context.annotation.SchedulCompleter;
import org.eto.fom.context.annotation.SchedulFactory;
import org.eto.fom.context.annotation.SchedulTerminator;
import org.eto.fom.context.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import example.fom.test.TestTask;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(remark = "定时批任务测试", fixedRate = 11)
public class BatchSchedulTest implements SchedulFactory<Long>, SchedulCompleter<Long>, SchedulTerminator {

	private static final Logger LOG = LoggerFactory.getLogger("BatchSchedulTest");
	
	@Override
	public Collection<TestTask> newSchedulTasks() throws Exception {
		List<TestTask> list = new ArrayList<>();
		for(int i = 0; i < 12; i++){
			list.add(new TestTask(i));
		}
		return list;
	}
	
	@Override
	public void onScheduleComplete(long schedulTimes, long schedulTime, List<Result<Long>> results) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(schedulTime);
		LOG.info( "第{}次在{}提交的任务全部完成，结果为{}", date, date, results);
	}
	
	@Override
	public void onScheduleTerminate(long schedulTimes, long lastTime) {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastTime);
		LOG.info("任务关闭，共执行{}次任务，最后一次执行时间为{}", schedulTimes, date);
	}
}
