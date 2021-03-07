package org.eto.fom.context.annotation;

import java.util.List;

import org.eto.fom.context.core.Result;

/**
 * 
 * @author shanhm
 *
 */
public interface SchedulCompleter<E> {

	/**
	 * schedule任务完成时触发
	 * @param schedulTimes schedul执行次数
	 * @param schedulTime schedul执行时间
	 * @param results 任务执行的结果集
	 * @throws Exception
	 */
	public void onScheduleComplete(long schedulTimes, long schedulTime, List<Result<E>> results) throws Exception;
}
