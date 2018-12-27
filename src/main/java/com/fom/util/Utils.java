package com.fom.util;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class Utils {
	
	private static String DELIM_START = "${";

	private static char   DELIM_STOP  = '}';

	private static int DELIM_START_LEN = 2;

	private static int DELIM_STOP_LEN  = 1;

	public static String parsePath(String val) throws IllegalArgumentException {
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		int j, k;
		while(true) {
			j = val.indexOf(DELIM_START, i);
			if(j == -1) {
				if(i==0) {
					return val;
				} else { 
					buffer.append(val.substring(i, val.length()));
					return buffer.toString();
				}
			} else {
				buffer.append(val.substring(i, j));
				k = val.indexOf(DELIM_STOP, j);
				if(k == -1) {
					throw new IllegalArgumentException('"' 
							+ val + "\" has no closing brace. Opening brace at position " + j + '.');
				} else {
					j += DELIM_START_LEN;
					String key = val.substring(j, k);
					String replacement = System.getProperty(key);
					if(replacement != null) {
						String recursiveReplacement = parsePath(replacement);
						buffer.append(recursiveReplacement);
					}
					i = k + DELIM_STOP_LEN;
				}
			}
		}
	}

	public static void close(AutoCloseable con){
		if(con == null){
			return;
		}
		
		try{
			con.close();
		}catch(Exception e){

		}
	}
	
}
