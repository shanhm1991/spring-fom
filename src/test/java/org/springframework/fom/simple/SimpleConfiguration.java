package org.springframework.fom.simple;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.fom.annotation.EnableFom;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Configuration
@ComponentScan("org.springframework.fom.simple")
@PropertySource("classpath:org/springframework/fom/simple/conf.properties")
@EnableFom
public class SimpleConfiguration {

}
