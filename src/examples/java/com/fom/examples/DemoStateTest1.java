package com.fom.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
	protected List<String> getTaskIdList() throws Exception {
		List<String> list = new ArrayList<String>();
		for(int i = 1; i < 50;i++){
			list.add("task-" + i);
		}
		return list;
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
