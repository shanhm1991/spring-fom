package org.eto.fom.boot.listener;

import java.io.File;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.eto.fom.boot.ServletUtil;
import org.eto.fom.context.ContextManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化容器，加载配置文件fom.xml
 * <br>&lt;fom&gt;
 * <br>&nbsp;&nbsp;&lt;fom-scan/&gt;
 * <br>&nbsp;&nbsp;&lt;includes&gt;
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;include/&gt;
 * <br>&nbsp;&nbsp;&lt;/includes&gt;
 * <br>&nbsp;&nbsp;&lt;contexts&gt;
 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&lt;context/&gt;
 * <br>&nbsp;&nbsp;&lt;/contexts&gt;
 * <br>&lt;/fom&gt;
 * <br>加载策略:
 * <br>1.加载cache中所有缓存的所有context
 * <br>2.加载主配置文件中的contexts节点
 * <br>3.依次加载主配置文件中的includes节点下包含的配置文件中的contexts节点
 * <br>4.扫描fom-scan节点配置所有包package下面的注解类
 * <br>如果出现同名name，以第一个加载成功的为准，当所有context加载完毕后依次启动
 * 
 * @author shanhm
 *
 */
@Component
class FomInitializer implements ApplicationRunner {

	//volatile只能保证引用的变化立即刷新，但系统对这个引用只有一次引用赋值操作
	private static volatile ServletContext servlet;
	
	static void set(ServletContext context){
		if(servlet != null){
			throw new UnsupportedOperationException();
		}
		servlet = context;
	}
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		init();
	}

	private void init() {
		initSysProperties(servlet);
		String xmlPath = ServletUtil.getContextPath(servlet.getInitParameter("fomConfigLocation"));
		ContextManager.load(xmlPath);
	}

	private void initSysProperties(ServletContext servlet){
		String cacheRoot = System.getProperty("cache.root");
		if(StringUtils.isBlank(cacheRoot)){
			cacheRoot = servlet.getRealPath("/WEB-INF/cache");
			if(StringUtils.isBlank(cacheRoot)){
				String root = System.getProperty("webapp.root");
				cacheRoot = root + File.separator + "cache";
			}  
			System.setProperty("cache.root", cacheRoot);
		}
		String configCache = System.getProperty("cache.context");
		if(StringUtils.isBlank(configCache)){
			configCache = cacheRoot + File.separator + "context";
			File history = new File(configCache + File.separator + "history");
			if(!history.exists()){
				history.mkdirs();
			}
			System.setProperty("cache.context", configCache);
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

}
