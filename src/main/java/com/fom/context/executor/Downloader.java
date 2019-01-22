package com.fom.context.executor;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import com.fom.context.exception.WarnException;
import com.fom.context.helper.Helper;
import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class Downloader implements Executor {
	
	protected final Logger log;
	
	protected final Helper helper;
	
	protected final String name;
	
	protected final String url;
	
	protected final String fileName;
	
	protected final String destPath;
	
	protected final boolean withTemp;
	
	protected final boolean isDelSrc;
	
	protected final String tempPath;
	
	protected final DecimalFormat numFormat  = new DecimalFormat("#.##");
	
	/**
	 * 
	 * @param name
	 * @param url
	 * @param destPath
	 * @param fileName
	 * @param withTemp
	 * @param isDelSrc
	 * @param helper
	 */
	public Downloader(String name, String url, String destPath, String fileName, 
			boolean withTemp, boolean isDelSrc, Helper helper) {
		this.name = name;
		this.url = url;
		this.destPath = destPath;
		this.fileName = fileName;
		this.helper = helper;
		this.withTemp = withTemp;
		if(withTemp){
			this.tempPath = System.getProperty("download.temp") + File.separator + name;
		}else{
			this.tempPath = destPath;
		}
		this.isDelSrc = isDelSrc;
		this.log = LoggerFactory.getLogger(name);
	}

	public final void exec() throws Exception {
		long sTime = System.currentTimeMillis();
		String path = destPath;
		if(withTemp){
			path = tempPath;
		}
		File file = new File(path + File.separator + fileName);
		
		helper.download(url, file);
		long size = file.length();
		log.info("下载文件结束(" + numFormat.format(size) + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		move();
	}
	
	protected void move() {
		if(!withTemp){
			return;
		}
		File file = new File(tempPath + File.separator + fileName);
		if(file.exists() && !file.renameTo(new File(destPath + File.separator + fileName))){
			throw new WarnException("文件移动失败:" + file.getName()); 
		}
	}
}
