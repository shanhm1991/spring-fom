package org.eto.fom.example;

import org.eto.fom.boot.FomConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude={
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class})
@Import({FomConfiguration.class})
@ComponentScan(basePackages = {"org.eto.fom"})
public class Boot {

	public static void main(String[] args) {
		SpringApplication.run(Boot.class, args);
	}
}
