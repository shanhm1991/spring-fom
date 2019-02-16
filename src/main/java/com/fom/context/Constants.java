package com.fom.context;

/**
 * 
 * @author shanhm
 *
 */
class Constants {
	
	/**
	 * 定时表达式
	 */
	public static final String CRON = "cron";

	/**
	 * 备注
	 */
	public static final String REMARK = "remark";

	/**
	 * 线程池任务队列长度
	 */
	public static final String QUEUESIZE = "queueSize";

	/**
	 * 线程池核心线程数
	 */
	public static final String THREADCORE = "threadCore";

	/**
	 * 线程池最大线程数
	 */
	public static final String THREADMAX = "threadMax";

	/**
	 * 线程池任务线程最长空闲时间
	 */
	public static final String ALIVETIME = "threadAliveTime";

	/**
	 * 线程池任务线程执行超时时间
	 */
	public static final String OVERTIME = "threadOverTime";

	/**
	 * 线程池任务线程如果超时是否中断
	 */
	public static final String CANCELLABLE = "cancellable";
	
	
	public static boolean validKey(String key){
		return !THREADCORE.equals(key) && !THREADMAX.equals(key) && !ALIVETIME.equals(key) 
				&& !OVERTIME.equals(key) && !QUEUESIZE.equals(key) && !CANCELLABLE.equals(key) && !CRON.equals(key) ;
	}

}
