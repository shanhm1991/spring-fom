package com.test;

import java.util.HashSet;
import java.util.Set;

import org.eto.fom.context.Context;
import org.eto.fom.context.FomContext;
import org.eto.fom.context.Task;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(cron="0/10 * * * * ?")
public class ContextTest extends Context {
	
	private static final long serialVersionUID = -4648914163608513224L;
	
	@SuppressWarnings("unchecked")
	@Override
	protected Set<SelfTask> scheduleBatchTasks() throws Exception {
		Set<SelfTask> set = new HashSet<>();
		set.add(new SelfTask("demoTask"));
		return set;
	}
	
	private static class SelfTask extends Task<Boolean> {

		public SelfTask(String taskId) {
			super(taskId);
		}

		@Override
		protected Boolean exec() throws Exception {
			System.out.println("task executing...");
			Thread.sleep(20000); 
			return true;
		}
		
	}

	public static void main(String[] args) {
		new ContextTest().startup();
	}
}
