package org.eto.fom.boot.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eto.fom.context.core.ContextManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 
 * @author shanhm
 *
 */
@Component
public class FomListener implements ServletContextListener, ApplicationRunner {
	
	/**
	 * 为了将fom放在spring加载完之后加载，先在现在contextInitialized中记下ServletContext，然后spring加载完之后再在run中取值，
	 * 注意的是FomContextListener在这里是两个实例，一个是tomcat创建的，一个是spring管理，所以这里用static来共享
	 */
	private static volatile ServletContext context;

	@Override
	public void contextInitialized(ServletContextEvent event) {
		context = event.getServletContext();
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		ContextManager.load(context.getRealPath(context.getInitParameter("fomConfigLocation")));
	} 
}
