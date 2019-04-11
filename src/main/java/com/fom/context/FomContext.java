package com.fom.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author shanhm
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FomContext {

	public String name() default "";
	
	public String remark() default "";

	public String cron() default "";
	
	public boolean execOnLoad() default true;
	
	public boolean stopWithNoCron() default false;

	public int threadCore() default 4;

	public int threadMax() default 10;

	public int threadAliveTime() default 30;

	public int threadOverTime() default 3600;
	
	public int queueSize() default 200;

	public boolean cancellable() default false;

}
