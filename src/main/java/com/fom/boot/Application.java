package com.fom.boot;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.log4j.PropertyConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.ComponentScan;

import com.fom.context.ContextUtil;

/**
 * 启动参数:-Dwebapp.root="E:\repository\fom\target\classes"<br>
 * 
 * @author shanhm1991
 * @date 2019年1月14日
 *
 */
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class})
@ComponentScan(basePackages = {"com.fom"})
public class Application implements ServletContextInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void onStartup(ServletContext context) throws ServletException {
		PropertyConfigurator.configure(context.getRealPath("/WEB-INF/log4j.properties"));
		ContextUtil.setContext(context);
		context.setInitParameter("fomConfigLocation", "/WEB-INF/fom.xml");
		context.setInitParameter("poolConfigLocation", "/WEB-INF/pool.xml");	
	}
}
