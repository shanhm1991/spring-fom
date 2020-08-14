package example.fom.onbatchcomplete;

import java.util.ArrayList;
import java.util.List;

import org.eto.fom.context.annotation.FomSchedulBatch;
import org.eto.fom.context.annotation.SchedulBatchFactory;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedulBatch(name = "BatchCompleteTest", cron = "0/15 * * * * ?")
public class BatchCompleteDemo implements SchedulBatchFactory {

	@SuppressWarnings("unchecked")
	@Override
	public List<DemoTask> creatTasks() throws Exception {
		List<DemoTask> list = new ArrayList<>(500);
		for(int i = 0; i < 500; i++){
			list.add(new DemoTask("task-" + i));
		}
		return list;
	}
}
