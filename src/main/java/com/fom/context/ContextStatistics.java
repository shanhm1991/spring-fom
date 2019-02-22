package com.fom.context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author shanhm
 *
 */
class ContextStatistics {

	static final long COSTLEVEL_1 = 1000;

	static final long COSTLEVEL_2 = 10000;

	static final long COSTLEVEL_3 = 60000;

	static final long COSTLEVEL_4 = 600000;

	static final long COSTLEVEL_5 = 3600000;

	static final long COSTMAX = -1;

	long successCount = 0; 

	long totalCost = 0;

	long minCost = 0;

	long maxCost = 0;

	AtomicLong failedCount = new AtomicLong(0);

	Map<String, Object> failedMap = new ConcurrentHashMap<>();

	Map<Long, AtomicLong> allCostMap = new LinkedHashMap<>();

	/**
	 * 保存一定天数的详细信息
	 */
	volatile int saveDay = 10;
	
	volatile long level1 = 10000;
	
	volatile long level2 = 10000;
	
	volatile long level3 = 60000;
	
	volatile long level4 = 600000;
	
	volatile long level5 = 3600000;
	
	LinkedList<String> daysHaveSaved = new LinkedList<>(); 
	
	Map<String, Queue<CostDetail>> dayCostMap = new ConcurrentHashMap<>();

	public ContextStatistics() {
		allCostMap.put(COSTLEVEL_1, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_2, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_3, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_4, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_5, new AtomicLong(0));
		allCostMap.put(COSTMAX, new AtomicLong(0));
	}

	public void successIncrease(String taskId, long cost){
		if(cost < COSTLEVEL_1){
			allCostMap.get(COSTLEVEL_1).incrementAndGet();
		}else if(cost < COSTLEVEL_2){
			allCostMap.get(COSTLEVEL_2).incrementAndGet();
		}else if(cost < COSTLEVEL_3){
			allCostMap.get(COSTLEVEL_3).incrementAndGet();
		}else if(cost < COSTLEVEL_4){
			allCostMap.get(COSTLEVEL_4).incrementAndGet();
		}else if(cost < COSTLEVEL_5){
			allCostMap.get(COSTLEVEL_5).incrementAndGet();
		}else{
			allCostMap.get(COSTMAX).incrementAndGet();
		}
		
		DateFormat format = new SimpleDateFormat("yyyyMMdd");
		String today = format.format(System.currentTimeMillis());
		Queue<CostDetail> queue = dayCostMap.get(today);
		//保证每天只判断一次
		if(queue == null){
			synchronized (daysHaveSaved) { 
				if(!daysHaveSaved.contains(today)){
					daysHaveSaved.add(today);
					queue = new ConcurrentLinkedQueue<>();
					dayCostMap.put(today, queue);
					//清除超出保存天数的数据,利用list的插入顺序
					while(daysHaveSaved.size() > saveDay){
						String day = daysHaveSaved.removeLast();
						dayCostMap.remove(day);
					}
				}
			}
		}
		
		CostDetail costDetail = new CostDetail();//TODO
		
		queue.offer(costDetail);

		failedMap.remove(taskId);
		synchronized (this) {
			successCount++;
			totalCost += cost;
			if(minCost == 0 || cost < minCost){
				minCost = cost;
			}
			if(cost > maxCost){
				maxCost = cost;
			}
		}
	}

	public void failedIncrease(String taskId, Throwable throwable){
		if(throwable == null){
			failedMap.put(taskId, "null");
		}else{
			failedMap.put(taskId, throwable);
		}
		failedCount.incrementAndGet();
	}

	public synchronized long getSuccess(){
		return successCount;
	}

	public long getFailed(){
		return failedCount.get();
	}

	public Map<String, Map<String, Object>> successDetail(){
		Map<String, Map<String, Object>> map = new HashMap<>();

		Map<String, Object> allMap = new HashMap<>();
		synchronized (this) {
			if(successCount == 0){
				return map;
			}
			allMap.put("successCount", successCount);
			allMap.put("totalCost", totalCost);
			allMap.put("minCost", minCost);
			allMap.put("maxCost", maxCost);
		}
		allMap.put("avgCost", (long)allMap.get("totalCost") / (long)allMap.get("successCount"));
		allMap.put("level1", allCostMap.get(COSTLEVEL_1).get());
		allMap.put("level2", allCostMap.get(COSTLEVEL_2).get());
		allMap.put("level3", allCostMap.get(COSTLEVEL_3).get());
		allMap.put("level4", allCostMap.get(COSTLEVEL_4).get());
		allMap.put("level5", allCostMap.get(COSTLEVEL_5).get());
		allMap.put("level6", allCostMap.get(COSTMAX).get());

		String[] dayArray = null;
		synchronized (daysHaveSaved) { 
			dayArray = new String[daysHaveSaved.size()];
			dayArray = daysHaveSaved.toArray(dayArray);
		}
		
		//算起始位置 TODO
		int index = 0;
		
		for(int i = 1;i <= 10 + 10;i++){
			Map<String, Object> dayMap = new HashMap<>();
			if(index + i > dayArray.length){
				dayMap.put("successCount", 0);
				dayMap.put("totalCost", 0);
				dayMap.put("minCost", 0);
				dayMap.put("maxCost", 0);
				dayMap.put("avgCost", 0);
				dayMap.put("level1", 0);
				dayMap.put("level2", 0);
				dayMap.put("level3", 0);
				dayMap.put("level4", 0);
				dayMap.put("level5", 0);
				dayMap.put("level6", 0);
				map.put("day" + i, dayMap);
				continue;
			}
			
		}


		map.put("all", allMap);
		return map;
	}

	public String failedDetail(){
		long fails = failedCount.get();
		if(fails == 0){
			return "0";
		}
		return fails + "(" + failedMap.size() + ")";
	}

	public static class CostDetail {

		public String id;

		public long createTime;

		public long startTime;

		public long cost;

	}
}
