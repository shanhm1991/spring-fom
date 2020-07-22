package com.examples.task;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.context.core.Task;

/**
 * 
 * @author shanhm1991
 *
 */
@FomContext(remark="测试")
public class Demo1 extends Context {

	private static final long serialVersionUID = -838223512003059760L;
	
	private Random random = new Random(10000);

	@SuppressWarnings("unchecked")
	@Override
	protected Collection<Task<Boolean>> scheduleBatch() throws Exception {
		Set<Task<Boolean>> set = new HashSet<>();
		for(int i = 1; i < 50;i++){
			set.add(new Task<Boolean>("demo1-" + i){
				@Override
				protected Boolean exec() throws Exception {
					Thread.sleep(random.nextInt(10000)); 
					return true;
				}
				
			});
		}
		return set;
	}

}
