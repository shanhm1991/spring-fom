package com.fom.util;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class IoUtil {
	
	public static final void close(AutoCloseable con){
		if(con == null){
			return;
		}
		
		try{
			con.close();
		}catch(Exception e){

		}
	}
	
}
