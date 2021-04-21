package org.springframework.fom;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public enum State {

	/**
	 * 初始化
	 */
	INITED("images/inited.png", "inited"),

	/**
	 * 正在运行
	 */
	RUNNING("images/running.png", "running"),

	/**
	 * 等待定时周期
	 */
	SLEEPING("images/sleeping.png", "waiting for next time"),

	/**
	 * 正在停止
	 */
	STOPPING("images/stopping.png", "stopping"),

	/**
	 * 已经停止
	 */
	STOPPED("images/stopped.png", "stopped");

	private String src;

	private String title;

	State(String src, String title){
		this.src = src;
		this.title = title;
	}

	public String src(){
		return src;
	}

	public String title(){
		return title;
	}

}
