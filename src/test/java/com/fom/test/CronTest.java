package com.fom.test;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.quartz.CronExpression;

public class CronTest {

	public static void main(String[] args) throws ParseException {
		CronExpression cron = new CronExpression("0 0 0/1 * * ?");
		Date nextDate = cron.getNextValidTimeAfter(new Date());
		System.out.println(new SimpleDateFormat("YYYYMMdd HH:mm:ss SSS").format(nextDate));
		
		
		File file = new File("E:/");
		
		System.out.println(Arrays.asList(file.list()));
	}
}
