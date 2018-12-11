package com.fom.util;

/**
 * 
 * @author shanhm1991
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
