package com.fom.util;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
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
