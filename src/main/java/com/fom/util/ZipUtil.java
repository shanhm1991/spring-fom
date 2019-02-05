package com.fom.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import net.lingala.zip4j.core.HeaderReader;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.InternalZipConstants;

/**
 * 
 * @author shanhm
 *
 */
public class ZipUtil {
	
	private static final Logger LOG = Logger.getRootLogger();
	
	/**
	 * 解压指定uri的zip文件到指定目录
	 * @param uri String
	 * @param destDir File
	 * @return zip length
	 * @throws Exception Exception
	 */
	public static final long unZip(String uri, File destDir) throws Exception {
		return unZip(new File(uri), destDir);
	}

	/**
	 * 解压zip文件到指定目录
	 * @param file File
	 * @param destDir File
	 * @return zip length
	 * @throws Exception Exception
	 */
	public static final long unZip(File file, File destDir) throws Exception{ 
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
					outStream = new FileOutputStream(new File(destDir + File.separator + entryName));
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

	/**
	 * 校验指定uri的zip文件是否合法
	 * @param uri String
	 * @return boolean
	 */
	public static final boolean valid(String uri){
		return valid(new File(uri));
	}
	
	/**
	 * 校验zip文件是否合法
	 * @param zip File
	 * @return boolean
	 */
	public static final boolean valid(File zip){
		if(zip == null || !zip.exists() || !zip.canRead()){
			return false;
		}
		
		RandomAccessFile raf = null;
		try{
			raf = new RandomAccessFile(zip, InternalZipConstants.READ_MODE);
			HeaderReader headerReader = new HeaderReader(raf);
			ZipModel zipModel = headerReader.readAllHeaders("UTF-8");
			if (zipModel != null) {
				zipModel.setZipFile(zip.getPath());
			}
			return true;
		}catch(Exception e){
			LOG.error("invalid zip[" + zip.getName() + "]", e); 
		}finally{
			IoUtil.close(raf); 
		}
		return false;
	}
	
	/**
	 * 将给定的InputStream写入给定的zipOutStream，并返回写入前InputStream的字节数
	 * @param entryName entryName
	 * @param in InputStream
	 * @param zipOutStream ZipOutputStream
	 * @return InputStream length
	 * @throws Exception Exception
	 */
	public static final long zipEntry(String entryName, 
			InputStream in, ZipOutputStream zipOutStream) throws Exception{
		BufferedInputStream buffer = null;
		try{
			buffer = new BufferedInputStream(in); 
			ZipEntry zipEntry = new ZipEntry(entryName);
			zipOutStream.putNextEntry(zipEntry);
			int count;
			int BUFFER = 8192;
			byte[] data = new byte[BUFFER];
			while((count=buffer.read(data, 0, BUFFER))!=-1){
				zipOutStream.write(data, 0, count);
			}
			return zipEntry.getSize();
		}finally{
			IoUtil.close(buffer);
		}
	}
	
	/**
	 * 获取zip压缩包中的文件名称集合
	 * @param file File
	 * @return name set
	 * @throws Exception Exception
	 */
	@SuppressWarnings("unchecked")
	public static final Set<String> getEntrySet(File file) throws Exception{
		Set<String> set = new HashSet<>();
		if(!valid(file)){
			return set;
		}
		
		net.lingala.zip4j.core.ZipFile zip = new net.lingala.zip4j.core.ZipFile(file);
		List<FileHeader> headers =  zip.getFileHeaders();
		for(FileHeader header : headers){
			set.add(header.getFileName());
		}
		return set;
	}
}