package example.fom.test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eto.fom.context.core.Context;
import org.eto.fom.context.core.ContextConfig;
import org.eto.fom.context.core.Result;
import org.eto.fom.context.core.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 */
public class Test {
	
	private static final Logger LOG = LoggerFactory.getLogger("test");

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		 test1();
	}
	
	/**
	 * 当成普通线程池来提交任务
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static void test1() throws InterruptedException, ExecutionException{
		Context<Long> context = new Context<>("test");
		Future<Result<Long>> future = context.submit(new TestTask(1));
		Result<Long> result = future.get();
		LOG.info("任务结束，结果为{}", result);
	}
	
	/**
	 * 当成CompletionService使用，提交一批任务，并在全部完成时做一些事情
	 */
	public static void test2() {
		Context<Long> context = new Context<Long>("test"){
			@Override
			public void onScheduleComplete(long batchTimes, long batchTime, List<Result<Long>> results) throws Exception {
				log.info( "第{}次在{}提交的任务全部完成，结果为{}", batchTimes, batchTime, results);
			}
		}; 
		
		List<TestTask> tasks = new ArrayList<>();
		for(int i = 0;i < 5; i++){
			tasks.add(new TestTask(i));
		} 
		context.submitBatch(tasks);
	}
	
	/**
	 * 手动创建并设置定时计划，定时创建并执行任务
	 * @throws InterruptedException 
	 */
	public static void test3() throws InterruptedException {
		Context<Long> context = new Context<Long>("test"){
			@Override
			public Task<Long> schedul() throws Exception {
				return new TestTask(1);
			}
			 
			@Override
			public void onScheduleTerminate(long schedulTimes, long lastTime) {
				String last = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastTime);
				log.info("定时任务关闭，共执行{}次任务，最后一次执行时间为{}", schedulTimes, last);
			}
		}; 
		
		ContextConfig config = context.getConfig();
		config.setCron("0/5 * * * * ?"); 
		
		context.startup();
		
		Thread.sleep(29000); 
		context.shutDown();
	}
	
	/**
	 * 手动创建并设置定时计划，定时创建并执行批任务
	 * @throws InterruptedException 
	 */
	public static void test4() throws InterruptedException {
		Context<Long> context = new Context<Long>("test"){
			@Override
			public List<TestTask> newSchedulTasks() throws Exception {
				List<TestTask> tasks = new ArrayList<>();
				for(int i = 0;i < 5; i++){
					tasks.add(new TestTask(i));
				} 
				return tasks;
			} 
			
			@Override
			public void onScheduleComplete(long batchTimes, long batchTime, List<Result<Long>> results) throws Exception {
				String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(batchTime);
				log.info( "第{}次在{}提交的任务全部完成，结果为{}", batchTimes, date, results);
			}
		}; 
		
		ContextConfig config = context.getConfig();
		config.setCron("0/5 * * * * ?"); 
		
		context.startup();
		
		Thread.sleep(30000); 
		context.shutDown();
	}
}
