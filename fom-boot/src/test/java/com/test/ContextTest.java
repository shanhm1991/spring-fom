package com.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.context.core.Task;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(cron="0/10 * * * * ?")
public class ContextTest extends Context {
	
	@SuppressWarnings("unchecked")
	@Override
	protected Collection<SelfTask> scheduleBatch() throws Exception {
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
