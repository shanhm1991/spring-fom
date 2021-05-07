package org.spring.fom.support.task.parse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.lingala.zip4j.core.HeaderReader;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.InternalZipConstants;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ZipUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ZipUtil.class);

	private static final int BUFFER = 8192;

	/**
	 * 解压指定uri的zip文件到指定目录
	 * @param uri String
	 * @param destDir File
	 * @return cost
	 * @throws Exception Exception
	 */
	public static long unZip(String uri, File destDir) throws Exception {
		return unZip(new File(uri), destDir);
	}

	/**
	 * 解压zip文件到指定目录
	 * @param zipFile zipFile
	 * @param descDir descDir
	 * @return cost
	 * @throws Exception Exception
	 */
	public static long unZip(File zipFile, File descDir) throws Exception {
		long stime = System.currentTimeMillis();
		try (ZipArchiveInputStream inputStream = getZipFile(zipFile)) {
			ZipArchiveEntry entry = null;
			while ((entry = inputStream.getNextZipEntry()) != null) {
				try(OutputStream os = 
						new BufferedOutputStream(new FileOutputStream(new File(descDir, entry.getName())))){
					IOUtils.copy(inputStream, os);
				} 
			}
		} 
		return System.currentTimeMillis() - stime;
	}

	private static ZipArchiveInputStream getZipFile(File zipFile) throws Exception {
		return new ZipArchiveInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
	}

	/**
	 * 校验指定uri的zip文件是否合法
	 * @param uri String
	 * @return boolean
	 */
	public static boolean valid(String uri){
		return valid(new File(uri));
	}

	/**
	 * 校验zip文件是否合法
	 * @param zip File
	 * @return boolean
	 */
	public static boolean valid(File zip){
		if(zip == null){
			LOG.error("invalid zip[null]."); 
			return false;
		}

		if(!zip.exists()){
			LOG.error("invalid zip[" + zip.getName() + "], not exist."); 
			return false;
		}

		if(!zip.canRead()){
			LOG.error("invalid zip[" + zip.getName() + "], can not read."); 
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
	public static long zipEntry(String entryName, 
			InputStream in, ZipOutputStream zipOutStream) throws Exception{
		BufferedInputStream buffer = null;
		try{
			buffer = new BufferedInputStream(in); 
			ZipEntry zipEntry = new ZipEntry(entryName);
			zipOutStream.putNextEntry(zipEntry);
			int count;
			byte[] data = new byte[BUFFER];

			long length = 0;
			while((count=buffer.read(data, 0, BUFFER))!=-1){
				length =+ count;
				zipOutStream.write(data, 0, count);
			}
			return length;
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
	public static Set<String> getEntrySet(File file) throws Exception{
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