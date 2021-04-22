package org.springframework.fom.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.fom.ScheduleConfig;
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

	@AliasFor(annotation = Component.class)
	String value() default "";

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
	long fixedRate() default ScheduleConfig.FIXED_RATE_DEFAULT;
	
	String fixedRateString() default "";

	/**
	 * 任务执行计划：距上一次任务结束时的时间（单位：秒）
	 * @return
	 */
	long fixedDelay() default ScheduleConfig.FIXED_DELAY_DEFAULT;
	
	String fixedDelayString() default "";

	/**
	 * 加载时是否启动任务，默认false
	 * @return
	 */
	boolean execOnLoad() default ScheduleConfig.EXECONLOAN_DEFAULT;
	
	String execOnLoadString() default "";

	/**
	 * 线程池核心线程数：default=1，min=1，max=2147483647
	 * @return
	 */
	int threadCore() default ScheduleConfig.THREAD_MIN;
	
	String threadCoreString() default "";

	/**
	 * 线程池最大线程数，default=1，min=1，max=2147483647
	 * @return
	 */
	int threadMax() default ScheduleConfig.THREAD_MIN;
	
	String threadMaxString() default "";

	/**
	 * 任务线程空闲存活时间（单位：秒）：default=20，min=1，max=2147483647
	 * @return
	 */
	int threadAliveTime() default ScheduleConfig.THREAD_ALIVETIME_DEFAULT;
	
	String threadAliveTimeString() default "";

	/**
	 * 任务队列长度，default=100，min=1，max=2147483647
	 * @return
	 */
	int queueSize() default ScheduleConfig.QUEUE_SIZE_DEFAULT;
	
	String queueSizeString() default "";

	/**
	 * 任务超时时间（单位：秒）：default=0（不限时），min=1，max=2147483647，
	 * @return
	 */
	int taskOverTime() default ScheduleConfig.TASK_OVERTIME_DEFAULT;
	
	String taskOverTimeString() default "";

}
