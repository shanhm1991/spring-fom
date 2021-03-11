package example.fom.fomcontext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.context.core.Result;

import example.fom.test.TestTask;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(fixedRate = 10)
public class BatchFomContextTest extends Context<Long> {

	@Override
	public List<TestTask> newSchedulTasks() throws Exception {
		List<TestTask> tasks = new ArrayList<>();
		for(int i = 0;i < 5; i++){
			tasks.add(new TestTask(i));
		} 
		return tasks;
	} 
	
	@Override
	public void onScheduleComplete(long batchTimes, long batchTime, List<Result<Long>> results) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(batchTime);
		log.info(" 第" + batchTimes + "次在" + date + "提交的任务全部完成");
	}
}
