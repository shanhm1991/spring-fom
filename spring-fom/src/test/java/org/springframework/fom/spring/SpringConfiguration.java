package org.springframework.fom.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.fom.annotation.EnableFom;

@Configuration
@ComponentScan("org.springframework.fom")
@PropertySource("classpath:org/springframework/fom/spring/conf.properties")
@EnableFom
public class SpringConfiguration {

}
