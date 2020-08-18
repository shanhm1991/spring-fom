package example.fom.onbatchcomplete;

import org.eto.fom.context.core.Task;

public class DemoTask extends Task<Void> {

	public DemoTask(String id) {
		super(id); 
	}

	@Override
	protected Void exec() throws Exception {
		Thread.sleep(5000);
		return null;
	}

}
