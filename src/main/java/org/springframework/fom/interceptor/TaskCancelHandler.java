package org.springframework.fom.interceptor;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public interface TaskCancelHandler {

	/**
	 * 任务取消处理
	 * @param taskId 任务id
	 * @param costTime 任务耗时
	 */
	void handleCancel(String taskId, long costTime);
}
