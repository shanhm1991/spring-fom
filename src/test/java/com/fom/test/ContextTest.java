package com.fom.test;

import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Task;
import com.fom.context.FomContext;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(cron="0/10 * * * * ?")
public class ContextTest extends Context {
	
	private static final long serialVersionUID = -4648914163608513224L;
	
	@Override
	protected List<String> getTaskIdList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("demoTask");
		return list;
	}

	@Override
	protected Task createTask(String sourceUri) throws Exception {
		return new SelfExecutor(sourceUri);
	}
	
	private static class SelfExecutor extends Task {

		public SelfExecutor(String sourceUri) {
			super(sourceUri);
		}

		@Override
		protected boolean exec() throws Exception {
			System.out.println("task executing...");
			Thread.sleep(20000); 
			return true;
		}
		
	}

	public static void main(String[] args) {
		new ContextTest().startup();
	}
}
