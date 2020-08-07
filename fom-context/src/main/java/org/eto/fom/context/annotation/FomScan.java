package org.eto.fom.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eto.fom.context.core.FomConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 
 * @author shanhm
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(FomConfiguration.class)
public @interface FomScan {

	String[] basePackages() default{};
}
