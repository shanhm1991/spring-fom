package com.fom.boot;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.ComponentScan;

import com.fom.context.ContextUtil;

/**
 * 启动参数:<br>
 * -Dwebapp.root="/"<br>
 * -Dcache.root="/WEB-INF/cache"<br>
 * -Dlog4jConfigLocation="/WEB-INF/log4j.properties"<br>
 * -DfomConfigLocation="/WEB-INF/fom.xml"<br>
 * -DpoolConfigLocation="/WEB-INF/pool.xml"<br>
 * 
 * @author shanhm
 * @date 2019年1月14日
 *
 */
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class})
@ComponentScan(basePackages = {"com.fom"})
public class Application implements ServletContextInitializer {

	public static void main(String[] args) {
		String rootPath = System.getProperty("webapp.root");
		if(StringUtils.isBlank(rootPath)){
			rootPath = Application.class.getResource("/").getPath();
			System.setProperty("webapp.root", rootPath);
		}
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void onStartup(ServletContext context) throws ServletException {
		String logPath = System.getProperty("log4jConfigLocation");
		if(StringUtils.isBlank(logPath)){
			logPath = context.getRealPath("/WEB-INF/log4j.properties");
			PropertyConfigurator.configure(logPath);
		}
		String fomPath = System.getProperty("fomConfigLocation");
		if(StringUtils.isBlank(fomPath)){
			fomPath = "/WEB-INF/fom.xml";
		}
		String poolpath = System.getProperty("poolConfigLocation");
		if(StringUtils.isBlank(poolpath)){
			poolpath = "/WEB-INF/pool.xml";
		}
		context.setInitParameter("fomConfigLocation", fomPath);
		context.setInitParameter("poolConfigLocation", poolpath);	
		ContextUtil.setContext(context);
	}
}
