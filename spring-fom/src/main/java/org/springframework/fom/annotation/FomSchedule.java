package org.springframework.fom.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface FomSchedule {
	
	/**
	 * 定时计划：cron
	 */
	public static final String CRON = "cron";

	/**
	 * 定时计划：fixedRate
	 */
	public static final String FIXED_RATE = "fixedRate";

	/**
	 * 定时计划：fixedDelay
	 */
	public static final String FIXED_DELAY = "fixedDelay";

	/**
	 * 备注
	 */
	public static final String REMARK = "remark";

	/**
	 * 线程池任务队列长度
	 */
	public static final String QUEUE_SIZE = "queueSize";

	/**
	 * 线程池核心线程数
	 */
	public static final String THREAD_CORE = "threadCore";

	/**
	 * 线程池最大线程数
	 */
	public static final String THREAD_MAX = "threadMax";

	/**
	 * 线程池任务线程最长空闲时间
	 */
	public static final String THREAD_ALIVETIME = "threadAliveTime";

	/**
	 * 任务超时时间
	 */
	public static final String TASK_OVERTIME = "taskOverTime";

	/**
	 * 启动时是否执行
	 */
	public static final String EXEC_ONLOAN = "execOnLoad";
	
	/**
	 * 是否检测任务冲突
	 */
	public static final String ENABLE_TASK_CONFLICT = "enableTaskConflict";
	
	/**
	 * 是否对任务结果进行统计
	 */
	public static final String ENABLE_TASKRESULT_STAT = "enableTaskResultStat";
	
	/**
	 * 是否中断超时的任务
	 */
	public static final String CANCEL_TASK_ONTIMEOUT = "cancelTaskOnTimeout";
	
	/**
	 * 是否对每个任务单独检测超时
	 */
	public static final String DETECT_TIMEOUT_ONEACHTASK = "detectTimeoutOnEachTask";
	
	/**
	 * Running状态时是否忽略执行请求
	 */
	public static final String IGNORE_EXECREQUEST_WHEN_RUNNING = "ignoreExecRequestWhenRunning";
	
	/**
	 * 加载时是否启动
	 */
	public static final String ENABLE = "enable";
	
	/**
	 * 线程数：默认1
	 */
	public static final int THREAD_CORE_DEFAULT = 1;

	/**
	 * 线程空闲存活时间：默认1
	 */
	public static final int THREAD_ALIVETIME_DEFAULT = 1;

	/**
	 * 任务队列长度：默认256
	 */
	public static final int QUEUE_SIZE_DEFAULT = 256;

	/**
	 * 任务队列长度：min
	 */
	public static final int QUEUE_SIZE_MIN = 1;

	/**
	 * 任务超时时间：default
	 */
	public static final int TASK_OVERTIME_DEFAULT = 0;

	/**
	 * 定时计划：fixedRate 默认值：0
	 */
	public static final int FIXED_RATE_DEFAULT = 0;

	/**
	 * 定时计划：fixedDelay 默认值：0
	 */
	public static final int FIXED_DELAY_DEFAULT = 0;

	/**
	 * 启动时默认不执行
	 */
	public static final boolean EXEC_ONLOAN_DEFAULT = false;
	
	/**
	 * 默认不检测任务冲突
	 */
	public static final boolean ENABLE_TASK_CONFLICT_DEFAULT = false;
	
	/**
	 * 默认对任务结果进行统计
	 */
	public static final boolean ENABLE_TASKRESULT_STAT_DEFAULT = true;
	
	/**
	 * 默认不中断超时的任务
	 */
	public static final boolean CANCEL_TASK_ONTIMEOUT_DEFAULT = false;
	
	/**
	 * 默认对每个任务单独检测超时
	 */
	public static final boolean DETECT_TIMEOUT_ONEACHTASK_DEFAULT = true;
	
	/**
	 * 默认Running状态时忽略执行请求
	 */
	public static final boolean IGNORE_EXECREQUEST_WHEN_RUNNING_DEFAULT = true;
	
	/**
	 * 默认加载启动
	 */
	public static final boolean ENABLE_DEFAULT = true;

	@AliasFor(annotation = Component.class)
	String value() default "";

	/**
	 * 是否启用
	 * @return
	 */
	boolean enable() default ENABLE_DEFAULT;
	
	String enableString() default "";
	
	/**
	 * 备注
	 * @return
	 */
	String remark() default "";

	/**
	 * 任务执行计划：时间表
	 * @return
	 */
	String cron() default "";

	/**
	 * 任务执行计划：距上一次任务开始时的时间（单位：秒）
	 * @return
	 */
	long fixedRate() default FIXED_RATE_DEFAULT;
	
	String fixedRateString() default "";

	/**
	 * 任务执行计划：距上一次任务结束时的时间（单位：秒）
	 * @return
	 */
	long fixedDelay() default FIXED_DELAY_DEFAULT;
	
	String fixedDelayString() default "";

	/**
	 * 加载时是否启动任务，默认false
	 * @return
	 */
	boolean execOnLoad() default EXEC_ONLOAN_DEFAULT;
	
	String execOnLoadString() default "";

	/**
	 * 线程池核心线程数：default=1，min=1，max=2147483647
	 * @return
	 */
	int threadCore() default THREAD_CORE_DEFAULT;
	
	String threadCoreString() default "";

	/**
	 * 线程池最大线程数，default=1，min=1，max=2147483647
	 * @return
	 */
	int threadMax() default THREAD_CORE_DEFAULT;
	
	String threadMaxString() default "";

	/**
	 * 任务线程空闲存活时间（单位：秒）：default=1，min=1，max=2147483647
	 * @return
	 */
	int threadAliveTime() default THREAD_ALIVETIME_DEFAULT;
	
	String threadAliveTimeString() default "";

	/**
	 * 任务队列长度，default=256，min=1，max=2147483647
	 * @return
	 */
	int queueSize() default QUEUE_SIZE_DEFAULT;
	
	String queueSizeString() default "";

	/**
	 * 任务超时时间（单位：秒）：default=0（不限时），min=1，max=2147483647，
	 * @return
	 */
	int taskOverTime() default TASK_OVERTIME_DEFAULT;
	
	String taskOverTimeString() default "";
	
	/**
	 * 是否统计任务结果
	 * @return
	 */
	boolean enableTaskResultStat() default ENABLE_TASKRESULT_STAT_DEFAULT;
	
	String enableTaskResultStatString() default "";
	
	/**
	 * 是否检测任务冲突
	 * @return
	 */
	boolean enableTaskConflict() default ENABLE_TASK_CONFLICT_DEFAULT; 
	
	String enableTaskConflictString() default "";
	
	/**
	 * 是否中断超时的任务
	 * @return
	 */
	boolean cancelTaskOnTimeout() default CANCEL_TASK_ONTIMEOUT_DEFAULT;
	
	String cancelTaskOnTimeoutString() default "";
	
	/**
	 * 是否单独对每个任务检测超时
	 * @return
	 */
	boolean detectTimeoutOnEachTask() default DETECT_TIMEOUT_ONEACHTASK_DEFAULT;
	
	String detectTimeoutOnEachTaskString() default "";
	
	/**
	 * Running状态时是否忽略执行请求
	 * @return
	 */
	boolean ignoreExecRequestWhenRunning() default IGNORE_EXECREQUEST_WHEN_RUNNING_DEFAULT;
	
	String ignoreExecRequestWhenRunningString() default "";

}
