package example.fom;

import org.eto.fom.context.annotation.FomScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;

/**
 * 
 * @author shanhm
 *
 */
@SpringBootApplication(exclude={
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		ServletWebServerFactoryAutoConfiguration.class})
@FomScan(basePackages="example.fom")
public class Boot {

	public static void main(String[] args) {
		SpringApplication.run(Boot.class, args);
	}
}
