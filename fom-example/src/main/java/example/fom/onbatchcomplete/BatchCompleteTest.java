package example.fom.onbatchcomplete;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.context.core.Result;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(cron = "0/15 * * * * ?", queueSize = 200)
public class BatchCompleteTest extends Context {
	
	@SuppressWarnings("unchecked")
	@Override
	protected List<DemoTask> scheduleBatch() throws Exception {
		List<DemoTask> list = new ArrayList<>(500);
		for(int i = 0; i < 500; i++){
			list.add(new DemoTask("task-" + i));
		}
		return list;
	}

	@SuppressWarnings("hiding")
	@Override
	protected <Void> void onBatchComplete(long batch, long batchTime, List<Result<Void>> results) {
		String time = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(batchTime);
		log.info(results.size() +  " tasks of batch[" + batch + "] submited on " + time  + " completed.");
	}
	
}
