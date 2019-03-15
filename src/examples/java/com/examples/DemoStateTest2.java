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

	@SuppressWarnings("unchecked")
	@Override
	protected Set<Task<Boolean>> scheduleBatchTasks() throws Exception {
		Set<Task<Boolean>> set = new HashSet<>();
		set.add(new Task<Boolean>("task2"){
			@Override
			protected Boolean exec() throws Exception {
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
