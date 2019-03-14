package com.examples;

import java.util.HashSet;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.FomContext;
import com.fom.context.Task;

/**
 * 
 * @author shanhm1991
 *
 */
@FomContext(remark="状态测试", threadMax=10)
public class DemoStateTest2 extends Context {

	private static final long serialVersionUID = -838223512003059760L;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Set<Task> scheduleBatchTasks() throws Exception {
		Set<Task> set = new HashSet<>();
		set.add(new Task<String>("task2"){
			@Override
			protected boolean exec() throws Exception {
				while(true){
					try{
						Thread.sleep(30000); 
					}catch(InterruptedException e){
						//ignore
					}
					
				}
			}
			
		});
		return set;
	}

}
