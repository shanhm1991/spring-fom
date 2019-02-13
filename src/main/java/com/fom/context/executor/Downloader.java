package com.fom.context.executor;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.Executor;
import com.fom.context.ResultHandler;
import com.fom.context.helper.DownloaderHelper;

/**
 * 根据sourceUri下载单个文件的执行器
 * 
 * @author shanhm
 *
 */
public final class Downloader extends Executor {

	private final String sourceName;

	private final String destPath;

	private final boolean isDelSrc;

	private final boolean isWithTemp;

	private final DownloaderHelper helper;

	private String downloadPath;

	private File downloadFile;

	/**
	 * @param destName 资源名称
	 * @param sourceUri 资源uri
	 * @param destPath 下载目的路径
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param isWithTemp 是否使用临时目录
	 * @param helper DownloaderHelper
	 */
	public Downloader(String destName, String sourceUri, String destPath, 
			boolean isDelSrc, boolean isWithTemp, DownloaderHelper helper) {
		super(sourceUri);
		if(StringUtils.isBlank(destName) || StringUtils.isBlank(sourceUri) || 
				StringUtils.isBlank(destPath) || helper == null) {
			throw new IllegalArgumentException(); 
		}
		this.sourceName = destName;
		this.destPath = destPath;
		this.isDelSrc = isDelSrc;
		this.isWithTemp = isWithTemp;
		this.helper = helper;
	}

	/**
	 * @param destName 资源名称
	 * @param sourceUri 资源uri
	 * @param destPath 下载目的路径
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param isWithTemp 是否使用临时目录
	 * @param helper DownloaderHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	public Downloader(String destName, String sourceUri, String destPath, 
			boolean isDelSrc, boolean isWithTemp, DownloaderHelper helper, ExceptionHandler exceptionHandler) {
		this(destName, sourceUri, destPath, isDelSrc, isWithTemp, helper);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param destName 资源名称
	 * @param sourceUri 资源uri
	 * @param destPath 下载目的路径
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param isWithTemp 是否使用临时目录
	 * @param helper DownloaderHelper
	 * @param resultHandler ResultHandler
	 */
	public Downloader(String destName, String sourceUri, String destPath, 
			boolean isDelSrc, boolean isWithTemp, DownloaderHelper helper, ResultHandler resultHandler) {
		this(destName, sourceUri, destPath, isDelSrc, isWithTemp, helper);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param destName 资源名称
	 * @param sourceUri 资源uri
	 * @param destPath 下载目的路径
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param isWithTemp 是否使用临时目录
	 * @param helper DownloaderHelper 
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public Downloader(String destName, String sourceUri, String destPath, 
			boolean isDelSrc, boolean isWithTemp, DownloaderHelper helper, 
			ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(destName, sourceUri, destPath, isDelSrc, isWithTemp, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	protected boolean onStart() throws Exception {
		File dest = new File(destPath);
		if(!dest.exists() && !dest.mkdirs()){
			log.error("directory create failed: " + dest); 
			return false;
		}

		if(!isWithTemp){
			this.downloadPath = destPath;
		}else{
			this.downloadPath = System.getProperty("cache.download") + File.separator + getName();
			File file = new File(downloadPath);
			if(!file.exists() && !file.mkdirs()){
				log.error("directory create failed: " + downloadPath); 
				return false;
			}
		}

		downloadFile = new File(downloadPath + File.separator + sourceName); 
		if(downloadFile.exists()){
			if(downloadFile.delete()){
				log.info("delete exist file: " + downloadFile.getPath()); 
			}else{
				log.warn("delete exist file failed: " + downloadFile.getPath()); 
				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean exec() throws Exception {
		long sTime = System.currentTimeMillis();
		helper.download(sourceUri, downloadFile);
		String size = new DecimalFormat("#.###").format(downloadFile.length());
		log.info("finish downlod(" + size + "KB), cost=" + (System.currentTimeMillis() - sTime) + "ms");
		return true;
	}

	@Override
	protected boolean onComplete() throws Exception {
		if(isWithTemp && downloadFile.exists() 
				&& !downloadFile.renameTo(new File(destPath + File.separator + downloadFile.getName()))){
			log.error("move file failed:" + downloadFile.getName());
			return false;
		}
		if(isDelSrc){ 
			int code = helper.delete(sourceUri);
			if(code < 200 || code > 207){
				log.error("delete file failed:" + sourceUri);
				return false;
			}
		}
		return true;
	}
}
