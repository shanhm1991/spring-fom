package com.fom.test.context;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;

@FomContext(cron="0/10 * * * * ?")
public class Test extends Context {
	
	private static final long serialVersionUID = -4648914163608513224L;
	private Logger log = Logger.getRootLogger();
	
	public Test(String name){
		super(name);
	}

	@Override
	protected List<String> getUriList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("task1");
		log.info("scan");
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
			System.out.println("12124124");
			Thread.sleep(60000); 
			return true;
		}
		
	}

	public static void main(String[] args) {
		Test t = new Test("safasfafqw");

		t.start();
	}
}
