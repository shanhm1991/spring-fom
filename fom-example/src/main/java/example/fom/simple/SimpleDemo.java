package example.fom.simple;

import java.util.concurrent.Future;

import org.eto.fom.context.core.ContextHelper;
import org.eto.fom.context.core.Result;
import org.eto.fom.context.core.Task;

/**
 * 
 * @author shanhm
 *
 */
public class SimpleDemo {

	public static void main(String[] args) throws Exception {
		
		Task<Boolean> task = new Task<Boolean>("SimpleTask"){
			@Override
			protected Boolean exec() throws Exception {
				Thread.sleep(5000); 
				return true;
			}
			
		};
		
		Future<Result<Boolean>> future = ContextHelper.submitTask("SimpleContext", task);
		System.out.println(future.get()); 
	}
}
