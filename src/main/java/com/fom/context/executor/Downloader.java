package com.fom.context.executor;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import com.fom.context.exception.WarnException;
import com.fom.context.executor.config.IDownloaderConfig;
import com.fom.context.executor.helper.DownloaderHelper;
import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class Downloader implements Executor {

	protected final Logger log;

	protected final String name;

	protected final DownloaderHelper helper;

	protected final String uri;

	protected final String fileName;

	protected final String destPath;

	protected final boolean withTemp;

	protected final boolean isDelSrc;

	protected final String tempPath;

	/**
	 * 
	 * @param name
	 * @param config
	 * @param helper
	 */
	public Downloader(String name, IDownloaderConfig config, DownloaderHelper helper) {
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
		this.helper = helper;

		this.uri = config.getUri();
		this.fileName = config.getDestName();
		this.destPath = config.getDestPath();
		this.isDelSrc = config.isDelSrc();
		this.withTemp = config.isWithTemp();
		if(withTemp){
			this.tempPath = System.getProperty("download.temp") + File.separator + name;
		}else{
			this.tempPath = destPath;
		}
		File path = new File(tempPath);
		if(!path.exists() && !path.mkdirs()){
			throw new IllegalArgumentException("无法创建临时目录:" + tempPath); 
		}
	}

	public final void exec() throws Exception {
		long sTime = System.currentTimeMillis();
		String path = destPath;
		if(withTemp){
			path = tempPath;
		}
		File file = new File(path + File.separator + fileName);

		helper.download(uri, file);
		long size = file.length();
		log.info("下载文件结束(" 
				+ new DecimalFormat("#.##").format(size) + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
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
