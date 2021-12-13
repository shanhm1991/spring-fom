package org.springframework.fom.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.fom.FomBeanDefinitionRegistrar;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(FomBeanDefinitionRegistrar.class)
public @interface EnableFom {

	boolean enableFomView() default true;
}
