package org.eto.fom.context.annotation;

/**
 * 
 * @author shanhm
 *
 */
public interface SchedulTerminator {

	/**
	 * schedule终结时触发
	 * @param schedulTimes 定时任务已经执行次数
	 * @param lastTime 最后一次执行时间
	 */
	public void onScheduleTerminate(long schedulTimes, long lastTime);
}
