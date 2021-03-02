package org.eto.fom.context.core;

/**
 * 
 * context状态表
 * 
 * @author shanhm
 *
 */
public enum State {

	/**
	 * 0:初始化
	 */
	INITED(0, "images/inited.png", "inited"),

	/**
	 * 1:正在运行
	 */
	RUNNING(1, "images/running.png", "running"),

	/**
	 * 2:等待任务运行结束
	 */
	@Deprecated
	WAITING(2, "images/waiting.png", "waiting for completion"),

	/**
	 * 3:等待定时周期
	 */
	SLEEPING(3, "images/sleeping.png", "waiting for next time"),

	/**
	 * 4:正在停止
	 */
	STOPPING(4, "images/stopping.png", "stopping"),

	/**
	 * 5:已经停止
	 */
	STOPPED(5, "images/stopped.png", "stopped");

	private int value;

	private String src;

	private String title;

	State(int value, String src, String title){
		this.value = value;
		this.src = src;
		this.title = title;
	}

	public int value(){
		return value;
	}

	public String src(){
		return src;
	}

	public String title(){
		return title;
	}

}
