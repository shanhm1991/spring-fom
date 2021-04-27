package org.springframework.fom;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ScheduleStatistics {

	private static final String STAT_LEVEL_1 = "lv1";

	private static final String STAT_LEVEL_2 = "lv2";

	private static final String STAT_LEVEL_3 = "lv3";

	private static final String STAT_LEVEL_4 = "lv4";

	private static final String STAT_LEVEL_5 = "lv5";

	private static final String STAT_DAY = "saveday";

	private static final int DEFAULT_STAT_LEVEL_1 = 1000;

	private static final int DEFAULT_STAT_LEVEL_2 = 10000;

	private static final int DEFAULT_STAT_LEVEL_3 = 60000;

	private static final int DEFAULT_STAT_LEVEL_4 = 600000;

	private static final int DEFAULT_STAT_LEVEL_5 = 3600000;

	private static final int DEFAULT_STAT_DAY = 7;

	private final AtomicLong success = new AtomicLong(0);

	private final AtomicLong failed = new AtomicLong(0);

	private final LinkedList<String> dayHasSaved = new LinkedList<>();

	// map<day, queue>
	private final Map<String, Queue<Result<?>>> successMap = new ConcurrentHashMap<>();

	private final Map<String, Result<?>> failedMap = new ConcurrentHashMap<>();

	private final Map<String, Integer> statConfigMap = new ConcurrentHashMap<>();

	public ScheduleStatistics(){
		statConfigMap.put(STAT_DAY, DEFAULT_STAT_DAY);
		statConfigMap.put(STAT_LEVEL_1, DEFAULT_STAT_LEVEL_1);
		statConfigMap.put(STAT_LEVEL_2, DEFAULT_STAT_LEVEL_2);
		statConfigMap.put(STAT_LEVEL_3, DEFAULT_STAT_LEVEL_3);
		statConfigMap.put(STAT_LEVEL_4, DEFAULT_STAT_LEVEL_4);
		statConfigMap.put(STAT_LEVEL_5, DEFAULT_STAT_LEVEL_5);
	}

	void record(Result<?> result){
		if(result.isSuccess()){
			recordSuccess(result);
		}else{
			recordFailed(result); 
		}
	}

	private void recordSuccess(Result<?> result){
		success.incrementAndGet();
		String day = new SimpleDateFormat("yyyy/MM/dd").format(System.currentTimeMillis());
		Queue<Result<?>> queue = successMap.get(day); 
		// 尽量避免使用同步，虽然api中免不了也有同步的使用
		if(queue == null){ 
			queue = new ConcurrentLinkedQueue<>();	
			Queue<Result<?>> exist = successMap.putIfAbsent(day, queue);
			if(exist == null){
				// dayHasSaved由每天第一个放入queue的线程负责检测，不存在多线程访问场景  
				// 虽然每次不是同一个线程访问，但一天只检测一次，线程安全问题暂且忽略
				dayHasSaved.add(day);
				while(dayHasSaved.size() > statConfigMap.get(STAT_DAY)){ 
					successMap.remove(dayHasSaved.removeLast());
				}
			}else{
				queue = exist;
			}
		}
		queue.add(result);
	}

	private void recordFailed(Result<?> result){
		failedMap.put(result.getTaskId(), result);
		failed.incrementAndGet();
	}

	public boolean setLevel(int lv1, int lv2, int lv3, int lv4, int lv5){
		if(lv1 > lv2 || lv2 > lv3 || lv3 > lv4 || lv4 > lv5){
			throw new IllegalArgumentException("invalid level: " + lv1 + " " + lv2 + " " + lv3 + " " + lv4 + " " + lv5);
		}

		synchronized(this){
			int level_1 = statConfigMap.get(STAT_LEVEL_1);
			int level_2 = statConfigMap.get(STAT_LEVEL_2);
			int level_3 = statConfigMap.get(STAT_LEVEL_3);
			int level_4 = statConfigMap.get(STAT_LEVEL_4);
			int level_5 = statConfigMap.get(STAT_LEVEL_5);
			if(level_1 == lv1 && level_2 == lv2 && level_3 == lv3 && level_4 == lv4 && level_5 == lv5){
				return false;
			}

			statConfigMap.put(STAT_LEVEL_1, lv1);
			statConfigMap.put(STAT_LEVEL_2, lv2);
			statConfigMap.put(STAT_LEVEL_3, lv3);
			statConfigMap.put(STAT_LEVEL_4, lv4);
			statConfigMap.put(STAT_LEVEL_5, lv5);
			return true;
		}
	}

	AtomicLong getSuccess() {
		return success;
	}

	AtomicLong getFailed() {
		return failed;
	}

	// endDay yyyy/MM/dd
	Map<String, Object> getSuccessStat(String endDay) throws ParseException {  
		int level_1;
		int level_2;
		int level_3;
		int level_4;
		int level_5;
		int saveDay;
		// 获取统计 与 统计配置设置需要同步，防止获取统计过程中，level状态被破坏
		synchronized(this){
			level_1 = statConfigMap.get(STAT_LEVEL_1);
			level_2 = statConfigMap.get(STAT_LEVEL_2);
			level_3 = statConfigMap.get(STAT_LEVEL_3);
			level_4 = statConfigMap.get(STAT_LEVEL_4);
			level_5 = statConfigMap.get(STAT_LEVEL_5);
			saveDay = statConfigMap.get(STAT_DAY);
		}
		
		Map<String, Map<String, Object>> statMap = new HashMap<>();
		int countLv1 = 0; 
		int countLv2 = 0; 
		int countLv3 = 0; 
		int countLv4 = 0; 
		int countLv5 = 0; 
		int countLv6 = 0; 
		long min = 0;
		long max = 0;
		long total = 0;
		int count = 0;
		// 遍历过程中数据依然在改变，但是统计结果不是必须要体现出来
		for(Entry<String, Queue<Result<?>>> entry : successMap.entrySet()){
			String day = entry.getKey();
			Queue<Result<?>> queue = entry.getValue();
			
			int dayCountLv1 = 0; 
			int dayCountLv2 = 0; 
			int dayCountLv3 = 0; 
			int dayCountLv4 = 0; 
			int dayCountLv5 = 0; 
			int dayCountLv6 = 0; 
			long dayMin = 0;
			long datMax = 0;
			long dayTotal = 0;
			int dayCount = 0;
			for(Result<?> result : queue){
				long cost = result.getCostTime();
				if(cost < level_1){
					dayCountLv1++;
				}else if(cost < level_2){
					dayCountLv2++;
				}else if(cost < level_3){
					dayCountLv3++;
				}else if(cost < level_4){
					dayCountLv4++;
				}else if(cost < level_5){
					dayCountLv5++;
				}else{
					dayCountLv6++;
				}
				
				dayCount++;
				dayTotal += cost;
				if(dayMin == 0){
					dayMin = cost;
				}else if(cost < dayMin){
					dayMin = cost;
				}
				if(cost > datMax){
					datMax = cost;
				}
			}
			
			Map<String, Object> dayMap = new HashMap<>();
			dayMap.put("day", day);
			dayMap.put("level1", dayCountLv1);
			dayMap.put("level2", dayCountLv2);
			dayMap.put("level3", dayCountLv3);
			dayMap.put("level4", dayCountLv4);
			dayMap.put("level5", dayCountLv5);
			dayMap.put("level6", dayCountLv6);
			dayMap.put("successCount", dayCount);
			dayMap.put("minCost", dayMin);
			dayMap.put("maxCost", datMax);
			dayMap.put("avgCost", dayCount > 0 ? dayTotal / dayCount : 0);
			
			statMap.put(day, dayMap);
			countLv1 += dayCountLv1;
			countLv2 += dayCountLv2;
			countLv3 += dayCountLv3;
			countLv4 += dayCountLv4;
			countLv5 += dayCountLv5;
			countLv6 += dayCountLv6;
			count += dayCount;
			total += dayTotal;
			if(min == 0){
				min = dayMin;
			}else if(dayMin < min){
				min = dayMin;
			}
			if(datMax > max){
				max = datMax;
			}
		}

		Map<String, Object> allMap = new HashMap<>();
		allMap.put("level1", countLv1);
		allMap.put("level2", countLv2);
		allMap.put("level3", countLv3);
		allMap.put("level4", countLv4);
		allMap.put("level5", countLv5);
		allMap.put("level6", countLv6);
		allMap.put("successCount", count);
		allMap.put("avgCost", count > 0 ? total / count : 0);
		allMap.put("minCost", min);
		allMap.put("maxCost", max);
		
		Map<String, Object> successMap = new HashMap<>();
		successMap.put(STAT_LEVEL_1, level_1);
		successMap.put(STAT_LEVEL_2, level_2);
		successMap.put(STAT_LEVEL_3, level_3);
		successMap.put(STAT_LEVEL_4, level_4);
		successMap.put(STAT_LEVEL_5, level_5);
		successMap.put(STAT_DAY, saveDay);
		successMap.put("all", allMap);
		
		DateFormat dateFormata = new SimpleDateFormat("yyyy/MM/dd");
		Calendar calendar = Calendar.getInstance();
		if(endDay != null){
			calendar.setTime(dateFormata.parse(endDay));
		}else{
			endDay = dateFormata.format(System.currentTimeMillis());
			calendar.setTime(new Date());
		}
		
		// endDay 往前固定取十天
		successMap.put("day", endDay);
		for(int i = 1; i <= 10; i++){
			Map<String, Object> dayMap = statMap.get(endDay);
			if(dayMap == null){
				dayMap = new HashMap<>();
				dayMap.put("successCount", 0);
				dayMap.put("minCost", 0);
				dayMap.put("maxCost", 0);
				dayMap.put("avgCost", 0);
				dayMap.put("level1", 0);
				dayMap.put("level2", 0);
				dayMap.put("level3", 0);
				dayMap.put("level4", 0);
				dayMap.put("level5", 0);
				dayMap.put("level6", 0);
				dayMap.put("day", endDay);
			}
			successMap.put("day" + i, dayMap);
			
			calendar.add(Calendar.DAY_OF_MONTH, -1);
			endDay = dateFormata.format(calendar.getTime()); 
		}
		return successMap;
	}
}
