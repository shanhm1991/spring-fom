package com.fom.test;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.quartz.CronExpression;

public class CronTest {

	public static void main(String[] args) throws ParseException, InterruptedException {
		CronExpression cron = new CronExpression("0 0 0/1 * * ?");
		Date nextDate = cron.getNextValidTimeAfter(new Date());
		System.out.println(new SimpleDateFormat("YYYYMMdd HH:mm:ss SSS").format(nextDate));
		
		
		File file = new File("E:/");
		
		System.out.println(Arrays.asList(file.list()));
		
		Lock lock = new ReentrantLock();
		Condition condition = lock.newCondition();
		
		
		new Thread(){
			public void run(){
				try {
					sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				lock.lock();
				try{
					condition.signalAll();
				}finally{
					lock.unlock();
				}
			}
		}.start();
		
		
		lock.lock();
		try{
			if(!condition.await(3000, TimeUnit.MILLISECONDS)){
				System.out.println(1);
			}else{
				System.out.println(2);
			}
		}finally{
			lock.unlock();
		}
	}
}
