package com.fom.context.executor;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;

import com.fom.context.Executor;
import com.fom.context.helper.DownloaderHelper;

/**
 * 根据资源uri下载单个文件的实现
 * 
 * @author shanhm
 *
 */
public final class Downloader extends Executor {

	private final String sourceUri;
	
	private final String destPath;
	
	private final boolean isDelSrc;

	private final boolean isWithTemp;
	
	private final DownloaderHelper helper;

	private final String downloadPath;
	
	private final File downloadFile;

	/**
	 * @param name 模块名称
	 * @param sourceName 资源名称
	 * @param sourceUri 资源uri
	 * @param destPath 下载目的路径
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param isWithTemp 是否使用临时目录
	 * @param helper DownloaderHelper
	 */
	public Downloader(String name, String sourceName, String sourceUri, String destPath, 
			boolean isDelSrc, boolean isWithTemp, DownloaderHelper helper) {
		super(name, sourceName);
		if(StringUtils.isBlank(sourceUri) || StringUtils.isBlank(destPath) || helper == null) {
			throw new IllegalArgumentException(); 
		}
		this.helper = helper;
		this.sourceUri = sourceUri;
		this.destPath = destPath;
		this.isDelSrc = isDelSrc;
		this.isWithTemp = isWithTemp;
		if(!isWithTemp){
			this.downloadPath = destPath;
		}else{
			this.downloadPath = System.getProperty("download.temp") + File.separator + name;
		}
		this.downloadFile = new File(downloadPath + File.separator + sourceName); 
	}
	
	@Override
	protected boolean onStart() throws Exception {
		File file = new File(downloadPath);
		if(!file.exists() && !file.mkdirs()){
			log.error("下载目录创建失败:" + downloadPath); 
			return false;
		}
		return true;
	}

	@Override
	protected boolean exec() throws Exception {
		long sTime = System.currentTimeMillis();
		helper.download(sourceUri, downloadFile);
		String size = new DecimalFormat("#.##").format(downloadFile.length());
		log.info("下载文件结束(" + size + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		return true;
	}

	@Override
	protected boolean onComplete() throws Exception {
		if(isWithTemp && downloadFile.exists() 
				&& downloadFile.renameTo(new File(destPath + File.separator + downloadFile.getName()))){
			log.error("文件移动失败:" + downloadFile.getName());
			return false;
		}
		if(isDelSrc && !helper.delete(sourceUri)){ 
			log.error("删除源文件失败:" + sourceUri);
			return false;
		}
		return true;
	}
}
