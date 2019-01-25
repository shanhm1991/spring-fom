package com.fom.context.executor;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fom.context.Executor;
import com.fom.context.exception.WarnException;
import com.fom.context.executor.helper.DownloaderHelper;
import com.fom.log.LoggerFactory;

/**
 * 根据sourceUri下载单个文件的实现
 * 
 * @author shanhm
 *
 */
public class Downloader implements Executor {

	private Logger log;

	private String sourceUri;

	private DownloaderConfig config;

	private DownloaderHelper helper;

	private String downloadPath;
	
	private File downloadFile;

	/**
	 * 
	 * @param name 模块名称
	 * @param sourceUri 下载资源uri
	 * @param config DownloaderConfig
	 * @param helper DownloaderHelper
	 */
	public Downloader(String name, String sourceUri, DownloaderConfig config, DownloaderHelper helper) {
		if(StringUtils.isBlank(name) || StringUtils.isBlank(sourceUri) || config == null || helper == null) {
			throw new IllegalArgumentException(); 
		}
		this.log = LoggerFactory.getLogger(name);
		this.sourceUri = sourceUri;
		this.config = config;
		this.helper = helper;
		this.downloadPath = config.getDestPath();
		if(config.isWithTemp()){
			downloadPath = System.getProperty("download.temp") + File.separator + name;
		}
		this.downloadFile = new File(downloadPath + File.separator + config.getSourceName(sourceUri)); 
	}

	public final void exec() throws Exception {
		File file = new File(downloadPath);
		if(!file.exists() && !file.mkdirs()){
			throw new IllegalArgumentException("下载目录创建失败:" + downloadPath); 
		}
		
		long sTime = System.currentTimeMillis();
		helper.download(sourceUri, downloadFile);
		String size = new DecimalFormat("#.##").format(downloadFile.length());
		log.info("下载文件结束(" + size + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		
		if(config.isWithTemp() && downloadFile.exists() 
				&& downloadFile.renameTo(new File(config.getDestPath() + File.separator + downloadFile.getName()))){
			throw new WarnException("文件移动失败:" + downloadFile.getName()); 
		}
	}
}
