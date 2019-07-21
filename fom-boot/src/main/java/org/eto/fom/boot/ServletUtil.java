package org.eto.fom.boot;

import javax.servlet.ServletContext;

/**
 * 
 * @author shanhm1991
 *
 */
public class ServletUtil {

	//volatile只能保证引用的变化立即刷新，但系统对这个引用只有一次引用赋值操作
	private static volatile ServletContext servlet;

	static void set(ServletContext context){
		if(servlet != null){
			throw new UnsupportedOperationException();
		}
		servlet = context;
	}
	
	/**
	 * Return the real path for a given virtual path, if possible; otherwise return <code>null</code>.
	 * @param path path
	 * @return context location
	 */
	public static String getContextPath(String path) {
		return servlet.getRealPath(path);
	}
}
