package org.springframework.fom.interceptor;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public interface ScheduleTerminator {

	/**
	 * 定时器关闭处理
	 * @param execTimes 执行次数
	 * @param lastExecTime 最后一次执行时间
	 */
	public void onScheduleTerminate(long execTimes, long lastExecTime);
}
