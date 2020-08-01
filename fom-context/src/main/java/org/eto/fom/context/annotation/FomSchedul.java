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
public @interface FomSchedul {

	public String name() default "";
	
	public String remark() default "";
	
	public String cron() default "";
	
	public boolean execOnLoad() default false;
	
	public boolean stopWithNoCron() default false;

	public int threadCore() default FomContext.CORE;

	public int threadMax() default FomContext.MAX;

	public int threadAliveTime() default FomContext.ALIVE;

	public int threadOverTime() default FomContext.OVER;
	
	public int queueSize() default FomContext.QUEUE;

	public boolean cancellable() default false;
}
