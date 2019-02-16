package com.fom.examples;

import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Task;
import com.fom.context.FomContext;

/**
 * 
 * @author shanhm1991
 *
 */
@FomContext(remark="状态测试")
public class DemoStateTest1 extends Context {

	private static final long serialVersionUID = -838223512003059760L;

	@Override
	protected List<String> getTaskIdList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("stateTest");
		return list;
	}

	@Override
	protected Task createTask(String sourceUri) throws Exception {
		
		return new Task(sourceUri){

			@Override
			protected boolean exec() throws Exception {
				
				Thread.sleep(40000);
				
				return true;
			}
			
		};
	}

}
