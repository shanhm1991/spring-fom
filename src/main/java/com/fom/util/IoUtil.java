package com.fom.util;

/**
 * 
 * @author shanhm
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
