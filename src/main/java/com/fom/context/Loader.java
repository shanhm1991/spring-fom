package com.fom.context;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * 
 * @author shanhm
 *
 */
class Loader {

	static URLClassLoader systemLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

	private static Method method;

	static{
		try {
			method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void load(List<File> files) throws Exception {
		synchronized (systemLoader) {
			URL[] array = systemLoader.getURLs();
			for(File file : files){
				String path = file.getPath();
				for(URL uri : array){
					if(uri.getPath().equals(path)){
						continue;
					}
					method.invoke(systemLoader, new Object[] { file.toURI().toURL() });
				}
			}
		}
	}

	public static void load(File file) throws Exception {
		synchronized (systemLoader) {
			URL[] array = systemLoader.getURLs();
			String path = file.getPath();
			for(URL uri : array){
				if(uri.getPath().equals(path)){
					return;
				}
			}
			method.invoke(systemLoader, new Object[] { file.toURI().toURL() });
		}
	}

}
