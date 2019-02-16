package com.fom.examples;

import java.util.ArrayList;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;

/**
 * 
 * @author shanhm1991
 *
 */
@FomContext(remark="状态测试")
public class DemoStateTest2 extends Context {

	private static final long serialVersionUID = -838223512003059760L;

	@Override
	protected List<String> getUriList() throws Exception {
		List<String> list = new ArrayList<String>();
		list.add("stateTest");
		return list;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		
		return new Executor(sourceUri){

			@Override
			protected boolean exec() throws Exception {
				
				while(true){
					
					try{
						Thread.sleep(2000); 
					}catch(InterruptedException e){
						//ignore
					}
					
				}
			}
			
		};
	}

}
