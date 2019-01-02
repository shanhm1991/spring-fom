package com.fom.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class ZipUtil {

	public static final long unZip(File file, File unzipDir) throws ZipException, Exception{ 
		long sTime = System.currentTimeMillis();
		ZipFile zip = null;
		try {
			zip = new ZipFile(file);
			for(Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();){
				ZipEntry entry  = (ZipEntry)entries.nextElement();
				String entryName = entry.getName();
				InputStream inStream = null;
				OutputStream outStream = null;
				try{
					inStream = zip.getInputStream(entry);
					outStream = new FileOutputStream(new File(unzipDir + File.separator + entryName));
					byte[] buff = new byte[1024];
					int len = 0;
					while((len = inStream.read(buff)) > 0){
						outStream.write(buff,0,len); 
					}
				}finally{
					IoUtil.close(inStream);
					IoUtil.close(outStream);
				}
			}
			return System.currentTimeMillis() - sTime;
		} finally{
			IoUtil.close(zip);
		}
	}
}
