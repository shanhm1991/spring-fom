package org.springframework.fom.annotation;

import java.lang.annotation.*;

/**
 *
 * @author shanhm1991@163.com
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Schedule {

    String cron() default "";

    long fixedDelay() default -1;

    String fixedDelayString() default "";

    long fixedRate() default -1;

    String fixedRateString() default "";
}
