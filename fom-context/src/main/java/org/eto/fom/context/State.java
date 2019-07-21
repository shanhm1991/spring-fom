package org.eto.fom.context;

/**
 * 
 * context状态表
 * 
 * @author shanhm
 *
 */
public enum State {
	
	/**
	 * 初始化
	 */
	INITED(0),
	
	/**
	 * 正在运行
	 */
	RUNNING(1),
	
	/**
	 * 等待任务运行结束
	 */
	WAITING(2),
	
	/**
	 * 等待定时周期
	 */
	SLEEPING(3),
	
	/**
	 * 正在停止
	 */
	STOPPING(4),
	
	/**
	 * 已经停止
	 */
	STOPPED(5);
	
	private int value;

	State(int value){
		this.value = value;
	}
	
	public int value(){
		return value;
	}
}
