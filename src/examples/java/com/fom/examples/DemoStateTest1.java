package com.fom.examples;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.FomContext;
import com.fom.context.Task;

/**
 * 
 * @author shanhm1991
 *
 */
@FomContext(remark="状态测试")
public class DemoStateTest1 extends Context {

	private static final long serialVersionUID = -838223512003059760L;
	
	private Random random = new Random(30000);

	@Override
	protected Set<String> getTaskIdSet() throws Exception {
		Set<String> set = new HashSet<String>();
		for(int i = 1; i < 50;i++){
			set.add("task-" + i);
		}
		return set;
	}

	@Override
	protected Task createTask(String taskId) throws Exception {
		
		return new Task(taskId){

			@Override
			protected boolean exec() throws Exception {
				
				Thread.sleep(random.nextInt(30000)); 
				
				return true;
			}
			
		};
	}

}
