package org.springframework.fom.interceptor;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public interface TaskTimeoutHandler {

	/**
	 * 处理超时任务
	 * @param taskId 任务id
	 * @param costTime 任务耗时
	 */
	void handleTimeout(String taskId, long costTime);
}
