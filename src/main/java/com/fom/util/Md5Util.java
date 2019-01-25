package com.fom.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 引入一个Md5工具类，修正线程安全问题
 * 
 * @author shanhm
 *
 */
public class Md5Util {

	private static final Logger LOG = LoggerFactory.getLogger("root");
	
	/**
	 * 默认的密码字符串组合，用来将字节转换成 16 进制表示的字符,
	 * apache校验下载的文件的正确性用的就是默认的这个组合
	 */
	protected static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };


	private static MessageDigest messagedigest = null;

	static {
		try {
			messagedigest = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			LOG.error("Md5初始化失败", e);
		}
	}

	public static final String getMd5(String s) {
		return getMd5(s.getBytes());
	}

	public static final String getMd5(byte[] bytes) {
		synchronized(messagedigest) {
			messagedigest.update(bytes);
			bytes = messagedigest.digest();
		}
		return bufferToHex(bytes);
	}

	public static final String getMd5(File file) throws IOException {		
		byte[] bytes = null;
		synchronized(messagedigest) {
			InputStream input = null;
			try{
				input = new FileInputStream(file);
				byte[] buffer = new byte[1024];
				int numRead = 0;
				while ((numRead = input.read(buffer)) > 0) {
					messagedigest.update(buffer, 0, numRead);
				}
			}finally{
				IoUtil.close(input);
			}
			bytes = messagedigest.digest();
		}
		return bufferToHex(bytes);
	}

	private static String bufferToHex(byte bytes[]) {
		return bufferToHex(bytes, 0, bytes.length);
	}

	private static String bufferToHex(byte bytes[], int m, int n) {
		StringBuffer stringbuffer = new StringBuffer(2 * n);
		int k = m + n;
		for (int l = m; l < k; l++) {
			appendHexPair(bytes[l], stringbuffer);
		}
		return stringbuffer.toString();
	}

	private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
		// 取字节中高 4 位的数字转换, >>> 为逻辑右移，将符号位一起右移,此处未发现两种符号有何不同 
		// 取字节中低 4 位的数字转换 
		char c0 = hexDigits[(bt & 0xf0) >> 4];
		char c1 = hexDigits[bt & 0xf];
		stringbuffer.append(c0);
		stringbuffer.append(c1);
	}
}

