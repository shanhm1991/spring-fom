package com.fom.context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

	long allCount = 0; 

	long allCost = 0;

	long allMin = 0;

	long allMax = 0;

	private Object lock_all = new Object();

	AtomicLong failedCount = new AtomicLong(0);

	Map<String, Result> failedMap = new ConcurrentHashMap<>();

	Map<Long, AtomicLong> allCostMap = new LinkedHashMap<>();

	/**
	 * 保存一定天数的详细信息
	 */
	volatile int saveDay = 10;

	long level1 = 1000;

	long level2 = 10000;

	long level3 = 60000;

	long level4 = 600000;

	long level5 = 3600000;

	private Object lock_level = new Object();

	LinkedList<String> daysHaveSaved = new LinkedList<>(); 

	Map<String, Queue<CostDetail>> successMap = new ConcurrentHashMap<>();

	public ContextStatistics() {
		allCostMap.put(COSTLEVEL_1, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_2, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_3, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_4, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_5, new AtomicLong(0));
		allCostMap.put(COSTMAX, new AtomicLong(0));
	}

	public void successIncrease(String taskId, long cost, long createTime, long startTime){
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
		Queue<CostDetail> queue = successMap.get(today);
		//保证每天只判断一次
		if(queue == null){
			synchronized (daysHaveSaved) { 
				if(!daysHaveSaved.contains(today)){
					daysHaveSaved.add(today);
					queue = new ConcurrentLinkedQueue<>();
					successMap.put(today, queue);
					//清除超出保存天数的数据,利用list的插入顺序
					while(daysHaveSaved.size() > saveDay){
						String day = daysHaveSaved.removeLast();
						successMap.remove(day);
					}
				}
			}
		}

		CostDetail costDetail = new CostDetail();
		costDetail.id = taskId;
		costDetail.cost = cost;
		costDetail.createTime = createTime;
		costDetail.startTime = startTime;
		queue.offer(costDetail);

		failedMap.remove(taskId);
		synchronized (lock_all) {
			allCount++;
			allCost += cost;
			if(allMin == 0 || cost < allMin){
				allMin = cost;
			}
			if(cost > allMax){
				allMax = cost;
			}
		}
	}

	public void failedIncrease(String taskId, Result result){
		failedMap.put(taskId, result);
		failedCount.incrementAndGet();
	}

	public long getSuccess(){
		synchronized(lock_all){
			return allCount;
		}
	}

	public long getFailed(){
		return failedCount.get();
	}

	public Map<String, Object> successDetail() throws Exception{ 
		Map<String, Object> map = new HashMap<>();
		Map<String, Object> all = new HashMap<>();
		synchronized (lock_all) {
			all.put("successCount", allCount);
			all.put("totalCost", allCost);
			all.put("minCost", allMin);
			all.put("maxCost", allMax);
		}

		if((long)all.get("successCount") == 0){ 
			all.put("successCount", 0);
			all.put("avgCost", 0);
			all.put("minCost", 0);
			all.put("maxCost", 0);
			all.put("level1", 0);
			all.put("level2", 0);
			all.put("level3", 0);
			all.put("level4", 0);
			all.put("level5", 0);
			all.put("level6", 0);
		}else{
			all.put("avgCost", (long)all.get("totalCost") / (long)all.get("successCount"));
			all.put("level1", allCostMap.get(COSTLEVEL_1).get());
			all.put("level2", allCostMap.get(COSTLEVEL_2).get());
			all.put("level3", allCostMap.get(COSTLEVEL_3).get());
			all.put("level4", allCostMap.get(COSTLEVEL_4).get());
			all.put("level5", allCostMap.get(COSTLEVEL_5).get());
			all.put("level6", allCostMap.get(COSTMAX).get());
		}
		map.put("all", all);
		dayDetail(map, new SimpleDateFormat("yyyyMMdd").format(System.currentTimeMillis())); 
		return map;
	}

	public boolean levelChange(long v1, long v2, long v3, long v4, long v5){
		synchronized(lock_level){
			if(v1 == level1 && v2 == level2
					&& v3 == level3 && v4 == level4 && v5 == level5){
				return false;
			}else{
				level1 = v1;
				level2 = v2;
				level3 = v3;
				level4 = v4;
				level5 = v5;
				return true;
			}
		}
	}

	public void dayDetail(Map<String, Object> map, String day) throws Exception{
		long level1 = 0;
		long level2 = 0;
		long level3 = 0;
		long level4 = 0;
		long level5 = 0;
		synchronized(lock_level){
			level1 = this.level1;
			level2 = this.level2;
			level3 = this.level3;
			level4 = this.level4;
			level5 = this.level5;
		}

		map.put("level1", level1);
		map.put("level2", level2);
		map.put("level3", level3);
		map.put("level4", level4);
		map.put("level5", level5);
		map.put("saveDay", saveDay);
		map.put("day", day);

		DateFormat format = new SimpleDateFormat("yyyyMMdd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(format.parse(day));

		Queue<CostDetail> queue = null;
		for(int i = 1;i <= 10;i++){
			Map<String, Object> dayMap = new HashMap<>();
			if(successMap.get(day) == null || (queue = successMap.get(day)).isEmpty()){
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
				dayMap.put("day", day);
			}else{
				long v1 = 0;
				long v2 = 0;
				long v3 = 0;
				long v4 = 0;
				long v5 = 0;
				long v6 = 0;
				long min = Long.MAX_VALUE;
				long max = 0;
				long total = 0;
				CostDetail[] array = new CostDetail[queue.size()];
				array = queue.toArray(array);
				for(CostDetail detail : array){
					if(detail.cost < level1){
						v1++;
					}else if(detail.cost < level2){
						v2++;
					}else if(detail.cost < level3){
						v3++;
					}else if(detail.cost < level4){
						v4++;
					}else if(detail.cost < level5){
						v5++;
					}else{
						v6++;
					}
					if(detail.cost < min){
						min = detail.cost;
					}
					if(detail.cost > max){
						max = detail.cost;
					}
					total += detail.cost;
				}
				dayMap.put("successCount", array.length);
				dayMap.put("minCost", min);
				dayMap.put("maxCost", max);
				dayMap.put("avgCost", total / array.length);
				dayMap.put("level1", v1);
				dayMap.put("level2", v2);
				dayMap.put("level3", v3);
				dayMap.put("level4", v4);
				dayMap.put("level5", v5);
				dayMap.put("level6", v6);
				dayMap.put("day", day);
			}
			map.put("day" + i, dayMap);
			calendar.add(Calendar.DAY_OF_MONTH, -1);
			day = format.format(calendar.getTime()); 
		}
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
