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

public class Test {

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		 test4();
	}
	
	/**
	 * 当成普通线程池来提交任务
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static void test1() throws InterruptedException, ExecutionException{
		Context<Long> context = new Context<>("test");
		Future<Result<Long>> future = context.submit(new TestTask(1));
		System.out.println(future.get()); 
	}
	
	/**
	 * 当成CompletionService使用，提交一批任务，并在全部完成时做一些事情
	 */
	public static void test2() {
		Context<Long> context = new Context<Long>("test"){
			@Override
			public void onScheduleComplete(long batchTimes, long batchTime, List<Result<Long>> results) throws Exception {
				String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
				String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(batchTime);
				System.out.println(now + " 第" + batchTimes + "次在时间" + date + "提交的任务全部完成，结果为" + results);
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
				String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
				String last = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastTime);
				System.out.println(now + " 定时任务关闭，共执行" + schedulTimes + "次任务，最后一次执行时间为" + last);
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
				String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis());
				String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(batchTime);
				System.out.println(now +" 第" + batchTimes + "次在时间" + date + "提交的任务全部完成");
			}
		}; 
		
		ContextConfig config = context.getConfig();
		config.setCron("0/5 * * * * ?"); 
		
		context.startup();
		
		Thread.sleep(30000); 
		context.shutDown();
	}
}
