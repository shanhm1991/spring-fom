package org.springframework.fom.test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 
 * @author shanhm1991@163.com
 *
 */ 
public class Test {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		new AnnotationConfigApplicationContext("org.springframework.fom.test");
	}
}
