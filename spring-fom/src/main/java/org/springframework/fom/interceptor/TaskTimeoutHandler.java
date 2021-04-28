package org.springframework.fom.interceptor;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public interface TaskTimeoutHandler {

	void handleTimeout(long costTime);
}
