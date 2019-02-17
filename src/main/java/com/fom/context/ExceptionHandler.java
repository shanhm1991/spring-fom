package com.fom.context;

/**
 * Task异常处理器
 * 
 * @see Task
 * 
 * @author shanhm
 *
 */
public interface ExceptionHandler {
	
	/**
	 * @param e Throwable
	 */
	void handle(Throwable e);

}
