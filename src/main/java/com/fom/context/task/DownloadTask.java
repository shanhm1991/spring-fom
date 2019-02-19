package com.fom.context.task;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.ResultHandler;
import com.fom.context.Task;
import com.fom.context.helper.DownloadHelper;

/**
 * 根据sourceUri下载单个文件的任务实现，以sourceUri作为task的id
 * <br>
 * <br>下载策略：
 * <br>1.检查下载目录是否存在，不存在则创建（如果使用临时目录，同时创建临时下载目录）
 * <br>2.检查下载目录下是否有同名文件（如果使用临时目录，则检查临时目录），如果有则先删除已存在的文件
 * <br>3.下载文件到下载目录（如果使用临时目录，则先下载到临时目录，再移到下载目录）
 * <br>4.决定是否删除源文件
 * <br>上述任何步骤失败或异常均会使任务提前失败结束
 * 
 * @see DownloadHelper
 * 
 * @author shanhm
 *
 */
public final class DownloadTask extends Task {

	private final String destName;

	private final String destPath;

	private final boolean isDelSrc;

	private final boolean isWithTemp;

	private final DownloadHelper helper;

	private String downloadPath;

	private File downloadFile;

	/**
	 * @param sourceUri 资源uri
	 * @param destName 下载文件命名
	 * @param destPath 目标下载目录
	 * @param isDelSrc 下载结束是否删除源文件
	 * @param isWithTemp 是否先下载到临时目录
	 * @param helper DownloaderHelper下载方法实现
	 */
	public DownloadTask(String sourceUri, String destName, String destPath, 
			boolean isDelSrc, boolean isWithTemp, DownloadHelper helper) {
		super(sourceUri);
		if(StringUtils.isBlank(sourceUri) || StringUtils.isBlank(destName) || StringUtils.isBlank(destPath) || helper == null) {
			throw new IllegalArgumentException(); 
		}
		this.destName = destName;
		this.destPath = destPath;
		this.isDelSrc = isDelSrc;
		this.isWithTemp = isWithTemp;
		this.helper = helper;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param destName 下载文件命名
	 * @param destPath 目标下载目录
	 * @param isDelSrc 下载结束是否删除源文件
	 * @param isWithTemp 是否先下载到临时目录
	 * @param helper DownloaderHelper下载方法实现
	 * @param exceptionHandler 异常处理器
	 */
	public DownloadTask(String sourceUri, String destName, String destPath, 
			boolean isDelSrc, boolean isWithTemp, DownloadHelper helper, ExceptionHandler exceptionHandler) {
		this(sourceUri, destName, destPath, isDelSrc, isWithTemp, helper);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param destName 下载文件命名
	 * @param destPath 目标下载目录
	 * @param isDelSrc 下载结束是否删除源文件
	 * @param isWithTemp 是否先下载到临时目录
	 * @param helper DownloaderHelper下载方法实现
	 * @param resultHandler 结果处理器
	 */
	public DownloadTask(String sourceUri, String destName, String destPath, 
			boolean isDelSrc, boolean isWithTemp, DownloadHelper helper, ResultHandler resultHandler) {
		this(sourceUri, destName, destPath, isDelSrc, isWithTemp, helper);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param destName 下载文件命名
	 * @param destPath 目标下载目录
	 * @param isDelSrc 下载结束是否删除源文件
	 * @param isWithTemp 是否先下载到临时目录
	 * @param helper DownloaderHelper下载方法实现
	 * @param exceptionHandler 异常处理器
	 * @param resultHandler 结果处理器
	 */
	public DownloadTask(String sourceUri, String destName, String destPath, 
			boolean isDelSrc, boolean isWithTemp, DownloadHelper helper, 
			ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(sourceUri, destName, destPath, isDelSrc, isWithTemp, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	protected boolean beforeExec() throws Exception {
		File dest = new File(destPath);
		if(!dest.exists() && !dest.mkdirs()){
			log.error("directory create failed: " + dest); 
			return false;
		}

		if(!isWithTemp){
			this.downloadPath = destPath;
		}else{
			if(StringUtils.isBlank(getContextName())){
				this.downloadPath = System.getProperty("cache.download");
			}else{
				this.downloadPath = System.getProperty("cache.download") + File.separator + getContextName();
			}
			File file = new File(downloadPath);
			if(!file.exists() && !file.mkdirs()){
				log.error("directory create failed: " + downloadPath); 
				return false;
			}
		}

		downloadFile = new File(downloadPath + File.separator + destName); 
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
		helper.download(id, downloadFile);
		String size = new DecimalFormat("#.###").format(downloadFile.length());
		if (log.isDebugEnabled()) {
			log.debug("finish downlod(" + size + "KB), cost=" + (System.currentTimeMillis() - sTime) + "ms");
		}
		return true;
	}

	@Override
	protected boolean afterExec() throws Exception {
		if(isWithTemp && downloadFile.exists() 
				&& !downloadFile.renameTo(new File(destPath + File.separator + downloadFile.getName()))){
			log.error("move file failed:" + downloadFile.getName());
			return false;
		}
		if(isDelSrc){ 
			int code = helper.delete(id);
			if(code < 200 || code > 207){
				log.error("delete file failed:" + id);
				return false;
			}
		}
		return true;
	}
}
