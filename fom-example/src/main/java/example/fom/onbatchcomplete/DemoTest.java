package example.fom.onbatchcomplete;

import java.util.ArrayList;
import java.util.List;

public class DemoTest {

	public static void main(String[] args) throws InterruptedException {
		BatchCompleteTest exec = new BatchCompleteTest();
		
		List<DemoTask> list = new ArrayList<>(500);
		for(int i = 0; i < 500; i++){
			list.add(new DemoTask("task-" + i));
		}
		
		exec.submitBatch(list);
	}
}
