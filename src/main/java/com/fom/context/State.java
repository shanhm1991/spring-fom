package com.fom.context;

/**
 * 
 * @author shanhm1991
 *
 */
enum State {
	
	/**
	 * 初始化
	 */
	inited(0),
	
	/**
	 * 正在运行
	 */
	running(1),
	
	/**
	 * 等待任务运行结束
	 */
	waiting(2),
	
	/**
	 * 等待定时周期
	 */
	sleeping(3),
	
	/**
	 * 正在停止
	 */
	stopping(4),
	
	/**
	 * 已经停止
	 */
	stopped(5);
	
	private int value;

	State(int value){
		this.value = value;
	}
	
	public int value(){
		return value;
	}
}
