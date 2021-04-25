package org.springframework.fom.boot.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.fom.annotation.EnableFom;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@EnableFom(enableFomView=true)
@SpringBootApplication
public class BootTest {

	public static void main(String[] args) {
		SpringApplication.run(BootTest.class, args);
	}
}
