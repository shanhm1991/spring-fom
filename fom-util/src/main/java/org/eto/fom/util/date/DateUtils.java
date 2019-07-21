package org.eto.fom.util.date;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * @author shanhm
 */
public class DateUtils {

	private static final int UNIT_SECOND = 1000;
	
	private static Queue<Calendar> calendarQueue = new ConcurrentLinkedQueue<>(); 
	
	private static ConcurrentMap<String, Queue<DateFormat>> dateFormatMap = new ConcurrentHashMap<>(); 
	
	private static Calendar getCalendar(){
		Calendar calendar = calendarQueue.poll();
		if(calendar != null){
			return calendar;
		}
		return Calendar.getInstance();
	}
	
	private static DateFormat getDateFormat(String format){
		Queue<DateFormat> queue = dateFormatMap.get(format);
		if(queue ==null){
			Queue<DateFormat> newQueue = new ConcurrentLinkedQueue<>(); 
			queue = dateFormatMap.putIfAbsent(format, newQueue);
			if(queue == null){
				queue = newQueue;
			}
		}
		
		DateFormat dateFormat = queue.poll();
		if(dateFormat == null){
			dateFormat = new SimpleDateFormat(format);
		}
		return dateFormat;
	}
	
	/**
	 * 根据当前日期和偏移量计算日期
	 * @param shift 偏移量
	 * @param calendarField 偏移类型
	 * @return
	 */
	public static Date dateShift(int shift, int calendarField) {
		Calendar calendar = getCalendar();
		calendar.setTime(new Date());
		calendar.add(calendarField, shift); 
		
		Date date = calendar.getTime();
		calendarQueue.offer(calendar);
		return date;
	}
	
	/**
	 * 根据当前日期和偏移量计算日期, 并设置时分秒
	 * @param shift 偏移量
	 * @param calendarField 偏移类型
	 * @param hour 时(24小时制)
	 * @param minute 分 
	 * @param second 秒
	 * @return
	 */
	public static Date dateShift(int shift, int calendarField, Integer hour, Integer minute, Integer second) {
		Calendar calendar = getCalendar();
		calendar.setTime(new Date());
		calendar.add(calendarField, shift); 
		if(hour != null){
			calendar.set(Calendar.HOUR_OF_DAY, hour);
		}
		if(minute != null){
			calendar.set(Calendar.MINUTE, minute);
		}
		if(second != null){
			calendar.set(Calendar.SECOND, second);
		}
		
		Date date = calendar.getTime();
		calendarQueue.offer(calendar);
		return date;
	}
	
	/**
	 * 
	 * @param time 日期时间字符串
	 * @param format 格式化字符串
	 * @return 描述
	 * @throws ParseException 转换异常
	 */
	public static Date dateParse(String time, String format) throws ParseException {
		DateFormat dateFormat = getDateFormat(format);
		try{
			return dateFormat.parse(time);
		}finally{
			dateFormatMap.get(format).offer(dateFormat);
		}
	}
	
	/**
	 * 格式化日期
	 * @param date 日期
	 * @param format 格式化字符串
	 * @return
	 */
	public static String dateFormat(Date date, String format) {
		DateFormat dateFormat = getDateFormat(format);
		return dateFormat.format(date);
	}
	
	/**
	 * 
	 * @param date 日期时间
	 * @return 描述
	 */
	public static long date2Second(Date date) { 
		return date.getTime() / UNIT_SECOND;
	}
}
