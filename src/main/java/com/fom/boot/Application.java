package com.fom.boot;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.ResourceUtils;

import com.fom.context.ContextUtil;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class})
@ComponentScan(basePackages = {"com.fom"})
public class Application implements ServletContextInitializer {
	
	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

	@Override
	public void onStartup(ServletContext context) throws ServletException {
		context.setInitParameter("fomConfigLocation", "/WEB-INF/fom.xml");
		context.setInitParameter("poolConfigLocation", "/WEB-INF/pool.xml");		
		ContextUtil.INSTANCE.setServletContext(context);
		try {
			System.out.println(new File("").getAbsolutePath()); 
			System.out.println(ResourceUtils.getURL("classpath:").getPath()); 
			System.out.println(ContextUtil.getResourceFile("/WEB-INF/fom.xml").getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
