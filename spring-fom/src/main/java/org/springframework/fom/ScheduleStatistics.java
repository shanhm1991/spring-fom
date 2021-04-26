package org.springframework.fom;

import java.text.SimpleDateFormat;
import java.util.Map;
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

	private final Object lock_level = new Object();

	private final Map<Long, Long> successCostMap = new ConcurrentHashMap<>();

	private final AtomicLong all = new AtomicLong(0);

	private final AtomicLong success = new AtomicLong(0);

	private final AtomicLong failed = new AtomicLong(0);
	
	// map<day, queue>
	private final Map<String, Queue<Result<?>>> successMap = new ConcurrentHashMap<>();

	private final Map<String, Result<?>> failedMap = new ConcurrentHashMap<>();

	private long level_1 = 1000;

	private long level_2 = 10000;

	private long level_3 = 60000;

	private long level_4 = 600000;

	private long level_5 = 3600000;

	public ScheduleStatistics(){

	}

	void record(Result<?> result){
		all.incrementAndGet();
		if(result.isSuccess()){
			recordSuccess(result);
		}else{
			recordFailed(result); 
		}
	}

	private void recordSuccess(Result<?> result){
		success.incrementAndGet();

		long cost = result.getCostTime();
		successCostMap.merge(cost, 1L, Long::sum);

		String day = new SimpleDateFormat("yyyyMMdd").format(System.currentTimeMillis());
		Queue<Result<?>> queue = successMap.get(day);
		if(queue == null){
			queue = successMap.computeIfAbsent(day, k -> new ConcurrentLinkedQueue<>());
		}
	}

	private void recordFailed(Result<?> result){
		failedMap.put(result.getTaskId(), result);
		failed.incrementAndGet();
	}

	public boolean setLevel(long lv1, long lv2, long lv3, long lv4, long lv5){
		if(lv1 > lv2 || lv2 > lv3 || lv3 > lv4 || lv4 > lv5){
			throw new IllegalArgumentException("invalid level: " + lv1 + " " + lv2 + " " + lv3 + " " + lv4 + " " + lv5);
		}
		synchronized(lock_level){
			if(level_1 == lv1 && level_2 == lv2 && level_3 == lv3 && level_4 == lv4 && level_5 == lv5){
				return false;
			}
			level_1 = lv1;
			level_2 = lv2;
			level_3 = lv3;
			level_4 = lv4;
			level_5 = lv5;
			return true;
		}
	}

	public Map<Long, Long> getSuccessCostMap() {
		return successCostMap;
	}

	public AtomicLong getAll() {
		return all;
	}

	public AtomicLong getSuccess() {
		return success;
	}

	public AtomicLong getFailed() {
		return failed;
	}

	public Map<String, Queue<Result<?>>> getSuccessMap() {
		return successMap;
	}
	
	
}
