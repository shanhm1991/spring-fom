package com.fom.test;

import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.FomContext;
import com.fom.context.Task;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(cron="0/10 * * * * ?")
public class ContextTest extends Context {
	
	private static final long serialVersionUID = -4648914163608513224L;
	
	@Override
	protected Set<Task> scheduleBatchTasks() throws Exception {
		Set<Task> set = new HashSet<>();
		set.add(new SelfTask("demoTask"));
		return set;
	}
	
	private static class SelfTask extends Task {

		public SelfTask(String taskId) {
			super(taskId);
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
