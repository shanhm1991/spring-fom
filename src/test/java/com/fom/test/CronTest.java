package com.fom.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.quartz.CronExpression;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class CronTest {

	public static void main(String[] args) throws ParseException, InterruptedException {
		CronExpression cron = new CronExpression("0 0 0/1 * * ?");
		Date nextDate = cron.getNextValidTimeAfter(new Date());
		System.out.println(new SimpleDateFormat("YYYYMMdd HH:mm:ss SSS").format(nextDate));
	}
}
