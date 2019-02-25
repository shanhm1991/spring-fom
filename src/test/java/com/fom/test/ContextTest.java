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
	protected Set<String> getTaskIdSet() throws Exception {
		Set<String> set = new HashSet<String>();
		set.add("demoTask");
		return set;
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
