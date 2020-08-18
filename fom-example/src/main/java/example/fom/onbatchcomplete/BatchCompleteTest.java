package example.fom.onbatchcomplete;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(fixedDelay = 47 , queueSize = 200)
public class BatchCompleteTest extends Context<Void> {
	
	public BatchCompleteTest(){
		Logger logger = LogManager.getLogger("BatchCompleteTest");
		logger.setLevel(Level.toLevel("DEBUG"));
	}
	
	@Override
	protected List<DemoTask> scheduleBatch() throws Exception {
		List<DemoTask> list = new ArrayList<>(500);
		for(int i = 0; i < 500; i++){
			list.add(new DemoTask("task-" + i));
		}
		return list;
	}
	
}
