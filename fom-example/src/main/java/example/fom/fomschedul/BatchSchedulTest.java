package example.fom.fomschedul;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.eto.fom.context.annotation.FomSchedul;
import org.eto.fom.context.annotation.SchedulCompleter;
import org.eto.fom.context.annotation.SchedulFactory;
import org.eto.fom.context.annotation.SchedulTerminator;
import org.eto.fom.context.core.Result;
import org.eto.fom.context.core.Task;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedul(remark = "定时批任务测试", fixedRate = 15)
public class BatchSchedulTest implements SchedulFactory<String>, SchedulCompleter<String>, SchedulTerminator {

	/**
	 * 如果是定时批任务，需要实现SchedulFactory接口即可
	 */
	@Override
	public Collection<TestTask> newSchedulTasks() throws Exception {
		List<TestTask> list = new ArrayList<>();
		for(int i = 0; i < 5; i++){
			list.add(new TestTask("BatchSchedulTest-" + i));
		}
		return list;
	}
	
	/**
	 * 如果希望在定时批任务完成时做一些处理，只需实现SchedulCompleter即可
	 */
	@Override
	public void onScheduleComplete(long schedulTimes, long schedulTime, List<Result<String>> results) throws Exception {
		System.out.println("在时间" + schedulTime + "，第" + schedulTimes + "次执行定时任务，任务结果为" + results);
	}
	
	/**
	 * 如果希望在终结定时任务时做一些处理，只需实现SchedulTerminator即可
	 */
	@Override
	public void onScheduleTerminate(long schedulTimes, long lastTime) {
		System.out.println("共执行过" + schedulTimes + "次定时任务，最后一次执行时间为" + lastTime);
	}

	private static class TestTask extends Task<String>{

		public TestTask(String id) {
			super(id);
		}

		@Override
		public String exec() throws Exception {
			Thread.sleep(RandomUtils.nextInt(1000, 3000));
			return id;
		}
		
	}

	
	
}
