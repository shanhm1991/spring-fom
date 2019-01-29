package com.fom.test;

import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;

@FomContext
public class Test extends Context {

	@Override
	protected List<String> getUriList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("task1");
		return list;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		return new SelfExecutor(sourceUri);
	}
	
	private static class SelfExecutor extends Executor {

		public SelfExecutor(String sourceUri) {
			super(sourceUri);
		}

		@Override
		protected boolean exec() throws Exception {
			Thread.sleep(3000); 
			return true;
		}
		
	}

	public static void main(String[] args) {
		Test t = new Test();

		t.start();
	}
}
