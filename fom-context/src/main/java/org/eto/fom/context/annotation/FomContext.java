package org.eto.fom.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eto.fom.context.core.ContextConfig;

/**
 * 
 * @author shanhm
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FomContext {

	/**
	 * 模块名称
	 * @return
	 */
	public String name() default "";
	
	/**
	 * 备注
	 * @return
	 */
	public String remark() default "";

	/**
	 * 任务执行计划：时间表
	 * @return
	 */
	public String cron() default "";
	
	/**
	 * 任务执行计划：距上一次任务开始时的时间（单位：秒）
	 * @return
	 */
	public int fixedRate() default 0;
	
	/**
	 * 任务执行计划：距上一次任务结束时的时间（单位：秒）
	 * @return
	 */
	public int fixedDelay() default 0;
	
	/**
	 * 加载时是否启动任务，默认false
	 * @return
	 */
	public boolean execOnLoad() default false;
	
	/**
	 * 如果没有配置执行计划（cron/fixedRate/fixedDelay）,是否关闭，默认false，这种场景通常是将Context作为线程池来用
	 * @return
	 */
	public boolean stopWithNoCron() default false;

	/**
	 * 线程池核心线程数，default=4，min=1，max=100
	 * @return
	 */
	public int threadCore() default ContextConfig.THREADCORE_DEFAULT;

	/**
	 * 线程池最大线程数，default=200，min=100，max=1000
	 * @return
	 */
	public int threadMax() default ContextConfig.THREADMAX_DEFAULT;

	/**
	 * 任务线程空闲存活时间（单位：秒）：default=30，min=5，max=300
	 * @return
	 */
	public int threadAliveTime() default ContextConfig.ALIVETIME_DEFAULT;

	/**
	 * 任务超时时间（单位：秒）：default=3600，min=1，max=86400，
	 * <p>如果任务超时了还没有结束，且cancellable=true，那么将在检测时像任务发送中断请求
	 * @return
	 */
	public int threadOverTime() default ContextConfig.OVERTIME_DEFAULT;
	
	/**
	 * 任务队列长度，default=2000，min=1，max=2147483647
	 * @return
	 */
	public int queueSize() default ContextConfig.QUEUESIZE_DEFAULT;

	/**
	 * 任务发生超时了是否取消
	 * @return
	 */
	public boolean cancellable() default false;

}
