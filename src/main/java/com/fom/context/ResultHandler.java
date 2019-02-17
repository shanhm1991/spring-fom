package com.fom.context;

/**
 * Task结果处理器
 * 
 * @see Task
 * 
 * @author shanhm
 *
 */
public interface ResultHandler {

	void handle(Result result) throws Exception;
}
