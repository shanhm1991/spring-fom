package com.fom.context;

/**
 * 
 * @author shanhm
 *
 */
public interface ResultHandler {
	
	/**
	 * @param result 任务执行结果
	 * @throws Exception
	 */
	void handle(boolean result) throws Exception;

}
