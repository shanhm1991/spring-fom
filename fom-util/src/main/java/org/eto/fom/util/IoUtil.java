package org.eto.fom.util;

import org.apache.commons.net.SocketClient;
import org.apache.log4j.Logger;

/**
 * 
 * @author shanhm
 *
 */
public class IoUtil {
	
	private static final Logger LOG = Logger.getLogger(IoUtil.class);
	
	public static void close(AutoCloseable con){
		if(con == null){
			return;
		}
		try{
			con.close();
		}catch(Exception e){
			LOG.error("", e);
		}
	}
	
	public static void close(SocketClient client){
		if(client == null){
			return;
		}
		try{
			client.disconnect();
		}catch(Exception e){
			LOG.error("", e);
		}
	}
	
}
