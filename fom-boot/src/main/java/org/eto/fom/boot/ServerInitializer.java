package org.eto.fom.boot;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 设置一些默认路径，可以通过启动参数设置:
 * -Dwebapp.root="/"
 * -Dcache.root="/cache"
 * -Dcache.context="/cache/context"
 * -Dcache.parse="/cache/parse"
 * -Dcache.download="/cache/download"
 * -DfomConfigLocation="/config/fom.xml"
 * -DpoolConfigLocation="/config/pool.xml"
 * 
 * @author shanhm
 *
 */
@Configuration
public class ServerInitializer implements ServletContextInitializer {
	
	@Value("${server.port:8080}")
	private int port;
	
	@Value("${server.servlet.context-path:/}")
	private String contextPath;

	@Override
	public void onStartup(ServletContext context) throws ServletException {
		String fomPath = System.getProperty("fomConfigLocation");
		if(StringUtils.isBlank(fomPath)){
			fomPath = "/config/fom.xml";
		}
		
		String poolpath = System.getProperty("poolConfigLocation");
		if(StringUtils.isBlank(poolpath)){
			poolpath = "/config/pool.xml";
		}
		
		context.setInitParameter("fomConfigLocation", fomPath);
		context.setInitParameter("poolConfigLocation", poolpath);	
		
		String root = context.getRealPath("");
		System.setProperty("webapp.root", root);
		
		String cacheRoot = System.getProperty("cache.root");
		if(StringUtils.isBlank(cacheRoot)){
			cacheRoot = root + File.separator + "cache";
			System.setProperty("cache.root", cacheRoot);
		}
		
		String contextCache = System.getProperty("cache.context");
		if(StringUtils.isBlank(contextCache)){
			contextCache = cacheRoot + File.separator + "context";
			File history = new File(contextCache + File.separator + "history");
			if(!history.exists()){
				history.mkdirs();
			}
			System.setProperty("cache.context", contextCache);
		}
		
		String parseCache = System.getProperty("cache.parse");
		if(StringUtils.isBlank(parseCache)){
			parseCache = cacheRoot + File.separator + "parse";
			File parse = new File(parseCache);
			if(!parse.exists()){
				parse.mkdirs();
			}
			System.setProperty("cache.parse", parseCache);
		}
		
		String downloadCache = System.getProperty("cache.download");
		if(StringUtils.isBlank(downloadCache)){
			downloadCache = cacheRoot + File.separator + "download";
			File download = new File(downloadCache);
			if(!download.exists()){
				download.mkdirs();
			}
			System.setProperty("cache.download", downloadCache);
		}
	}
	
	@Bean
	public ServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
			@Override
			protected void postProcessContext(Context context) {
				super.postProcessContext(context);
				String root = System.getProperty("webapp.root");
				if(StringUtils.isBlank(root)){
					root = ClassLoader.getSystemResource(".").getPath(); //默认根路径
					String os = System.getProperty("os.name");
					if(os.toLowerCase().indexOf("windows") != -1){
						root = root.substring(1);
					}
					System.setProperty("webapp.root", root);
				}
				context.setDocBase(root); 
				context.setPath(contextPath);
			}
		};
		
		TomcatConnectorCustomizer connector = new TomcatConnectorCustomizer(){
			@Override
			public void customize(Connector connector) {
				connector.setPort(port);
			}
		};
		
		tomcat.addConnectorCustomizers(connector); 
		return tomcat;
	}
}
