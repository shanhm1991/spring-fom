package com.fom.util;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
public class IoUtils {

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
