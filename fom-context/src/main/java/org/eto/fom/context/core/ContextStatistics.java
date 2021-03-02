package org.eto.fom.context.core;

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
 * context的统计信息管理
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
	
	static final int SAVEDAY = 10;

	long allCount = 0; 

	long allCost = 0;

	long allMin = 0;

	long allMax = 0;

	private final Object lock_all = new Object();

	AtomicLong failedCount = new AtomicLong(0);

	Map<String, Result<?>> failedMap = new ConcurrentHashMap<>();

	Map<Long, AtomicLong> allCostMap = new LinkedHashMap<>();

	/**
	 * 保存一定天数的详细信息
	 */
	volatile int saveDay = SAVEDAY;

	long level1 = COSTLEVEL_1;

	long level2 = COSTLEVEL_2;

	long level3 = COSTLEVEL_3;

	long level4 = COSTLEVEL_4;

	long level5 = COSTLEVEL_5;

	private final Object lock_level = new Object();

	final LinkedList<String> daysHaveSaved = new LinkedList<>();

	ConcurrentHashMap<String, Queue<CostDetail>> successMap = new ConcurrentHashMap<>();

	public ContextStatistics() {
		allCostMap.put(COSTLEVEL_1, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_2, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_3, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_4, new AtomicLong(0));
		allCostMap.put(COSTLEVEL_5, new AtomicLong(0));
		allCostMap.put(COSTMAX, new AtomicLong(0));
	}
	
	public void statistics(Result<?> result){
		if(result.isSuccess()){
			successIncrease(result.getTaskId(), result.getCostTime(), result.getCreateTime(), result.getStartTime(), result.getContent());
		}else{
			failedIncrease(result.getTaskId(), result); 
		}
	}

	private void successIncrease(String taskId, long cost, long createTime, long startTime, Object content){
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
				}else{
					queue = successMap.get(today);
				}
			}
		}

		CostDetail costDetail = new CostDetail();
		costDetail.id = taskId;
		costDetail.cost = cost;
		costDetail.createTime = createTime;
		costDetail.startTime = startTime;
		costDetail.result = content;
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

	private void failedIncrease(String taskId, Result<?> result){
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

	public int getfailedDetails(){
		return failedMap.size();
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
		long l1;
		long l2;
		long l3;
		long l4;
		long l5;
		synchronized(lock_level){
			l1 = this.level1;
			l2 = this.level2;
			l3 = this.level3;
			l4 = this.level4;
			l5 = this.level5;
		}

		map.put("level1", l1);
		map.put("level2", l2);
		map.put("level3", l3);
		map.put("level4", l4);
		map.put("level5", l5);
		map.put("saveDay", saveDay);
		map.put("day", day);

		DateFormat format = new SimpleDateFormat("yyyyMMdd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(format.parse(day));

		for(int i = 1;i <= saveDay;i++){
			Map<String, Object> dayMap = new HashMap<>();
			Queue<CostDetail> queue = successMap.get(day);
			if(successMap.get(day) == null || queue.isEmpty()){
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
					if(detail.cost < l1){
						v1++;
					}else if(detail.cost < l2){
						v2++;
					}else if(detail.cost < l3){
						v3++;
					}else if(detail.cost < l4){
						v4++;
					}else if(detail.cost < l5){
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

	public static class CostDetail {

		public String id;

		public long createTime;

		public long startTime;

		public long cost;
		
		public Object result;

	}
}
