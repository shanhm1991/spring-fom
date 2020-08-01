package org.eto.fom.context.annotation;

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
	
	public static final int CORE = 4;
	
	public static final int MAX = 10;
	
	public static final int ALIVE = 30;
	
	public static final int OVER = 3600;
	
	public static final int QUEUE = 200;

	public String name() default "";
	
	public String remark() default "";

	public String cron() default "";
	
	public boolean execOnLoad() default false;
	
	public boolean stopWithNoCron() default false;

	public int threadCore() default CORE;

	public int threadMax() default MAX;

	public int threadAliveTime() default ALIVE;

	public int threadOverTime() default OVER;
	
	public int queueSize() default QUEUE;

	public boolean cancellable() default false;

}
