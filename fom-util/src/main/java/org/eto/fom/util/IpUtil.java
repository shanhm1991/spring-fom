package org.eto.fom.util;

/**
 * 
 * @author shanhm
 * 
 */
public class IpUtil {

	private static final int IP_LEN = 4;

	private static final int IP_INDEX_0 = 0;

	private static final int IP_INDEX_1 = 1;

	private static final int IP_INDEX_2 = 2;

	private static final int IP_INDEX3 = 3;

	private static final int IP_SHIFT_0 = 24;

	private static final int IP_SHIFT_1 = 16;

	private static final int IP_SHIFT_2 = 8;

	/**
	 * ip字符串转为数值
	 * @param ip
	 * @return
	 */
	public static long ip2Long(String ip){
		long[] array = new long[IP_LEN];  
		int position1 = ip.indexOf('.');   
		int position2 = ip.indexOf('.', position1 + 1);  
		int position3 = ip.indexOf('.', position2 + 1);  
		array[IP_INDEX_0] = Long.parseLong(ip.substring(0, position1));  
		array[IP_INDEX_1] = Long.parseLong(ip.substring(position1 + 1, position2));  
		array[IP_INDEX_2] = Long.parseLong(ip.substring(position2 + 1, position3));  
		array[IP_INDEX3] = Long.parseLong(ip.substring(position3 + 1));  
		return (array[IP_INDEX_0] << IP_SHIFT_0) 
				+ (array[IP_INDEX_1] << IP_SHIFT_1) 
				+ (array[IP_INDEX_2] << IP_SHIFT_2) 
				+ array[IP_INDEX3];
	}
}
