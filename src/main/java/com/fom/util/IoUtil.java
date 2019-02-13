package com.fom.util;

import org.apache.commons.net.SocketClient;

/**
 * 
 * @author shanhm
 *
 */
public class IoUtil {
	
	public static void close(AutoCloseable con){
		if(con == null){
			return;
		}
		try{
			con.close();
		}catch(Exception e){

		}
	}
	
	public static void close(SocketClient client){
		if(client == null){
			return;
		}
		try{
			client.disconnect();
		}catch(Exception e){

		}
	}
	
}
