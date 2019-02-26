package com.fom.context;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

/**
 * 
 * @author shanhm
 *
 */
class Loader {
	
	static URLClassLoader systemLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
	
	private static Set<String> classpathSet = new HashSet<>();

	private static String libPath = ClassLoader.getSystemResource("").getPath() + File.separator + "lib";
	
	private static Method method;

	static{
		try {
			method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		URL[] array = systemLoader.getURLs();
		for(URL uri : array){
			classpathSet.add(uri.getPath());
		}
	}
	
	public static void refreshClassPath() throws Exception {
		File libRoot = new File(libPath);
		if(!libRoot.exists()){
			return;
		}
		
		File[] jarArray = libRoot.listFiles();
		if(ArrayUtils.isEmpty(jarArray)){
			return;
		} 
		
		synchronized (classpathSet) {
			for(File jar : jarArray){
				String path = jar.getPath();
				if(!classpathSet.contains(path)){
					method.invoke(systemLoader, new Object[] { jar.toURI().toURL() });
					classpathSet.add(path);
				}
			}
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

	public static void loop(File file, List<File> list) {
		String path = file.getPath();
		if (file.isDirectory()) {
			File[] array = file.listFiles();
			for (File sub : array) {
				loop(sub, list);
			}
		}else if(path.endsWith("jar")){
			list.add(file);
		}
	}
}
