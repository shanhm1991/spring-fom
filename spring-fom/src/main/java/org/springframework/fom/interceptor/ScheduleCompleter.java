package org.springframework.fom.interceptor;

import java.util.List;

import org.springframework.fom.Result;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public interface ScheduleCompleter<E> {

	/**
	 * schedule任务完成时触发
	 * @param execTimes 执行次数
	 * @param lastExecTime 本次执行时间
	 * @param results 结果集
	 * @throws Exception
	 */
	public void onScheduleComplete(long execTimes, long lastExecTime, List<Result<E>> results) throws Exception;
}
