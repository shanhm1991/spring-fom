package com.fom.context.executor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.Executor;
import com.fom.context.ResultHandler;
import com.fom.context.helper.ZipDownloaderHelper;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 根据sourceUri列表下载并打包成zip文件的执行器
 * 
 * @author shanhm
 *
 */
public class ZipDownloader extends Executor {
	
	private final DecimalFormat numFormat  = new DecimalFormat("#.###");
	
	protected final ZipDownloaderHelper helper;

	protected final List<String> uriList;
	
	protected final String destPath;
	
	protected final int zipEntryMax;
	
	protected final long zipSizeMax;
	
	protected final boolean isDelSrc;
	
	protected final String zipName;

	private String downloadPath;

	private File downloadZip; 

	//downloadZip的命名序号
	private int index;

	//downloadZip中真实下载的文件名称集合
	private Set<String> entrySet;

	/**
	 * @param zipName 打包zip的名称(不带后缀)
	 * @param uriList 资源uri列表
	 * @param destPath 下载目的路径
	 * @param zipEntryMax 打包zip的最大文件数(真实下载的文件)
	 * @param zipSizeMax 打包zip的最大字节数
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param helper ZipDownloaderHelper
	 */
	public ZipDownloader(String zipName, List<String> uriList, String destPath, 
			int zipEntryMax, long zipSizeMax, boolean isDelSrc, ZipDownloaderHelper helper) {
		super(zipName);
		if(StringUtils.isBlank(zipName) || helper == null || uriList == null) {
			throw new IllegalArgumentException();
		}
		
		this.zipName = zipName;
		this.helper = helper;
		this.uriList = uriList;
		this.destPath = destPath;
		this.zipEntryMax = zipEntryMax;
		this.zipSizeMax = zipSizeMax;
		this.isDelSrc = isDelSrc;
	}
	
	/**
	 * @param zipName 打包zip的名称(不带后缀)
	 * @param uriList 资源uri列表
	 * @param destPath 下载目的路径
	 * @param zipEntryMax 打包zip的最大文件数(真实下载的文件)
	 * @param zipSizeMax 打包zip的最大字节数
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param helper ZipDownloaderHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	public ZipDownloader(String zipName, List<String> uriList, String destPath, 
			int zipEntryMax, long zipSizeMax, boolean isDelSrc, 
			ZipDownloaderHelper helper, ExceptionHandler exceptionHandler) {
		this(zipName, uriList, destPath, zipEntryMax, zipSizeMax, isDelSrc, helper);
		this.exceptionHandler = exceptionHandler;
	}
	
	/**
	 * @param zipName 打包zip的名称(不带后缀)
	 * @param uriList 资源uri列表
	 * @param destPath 下载目的路径
	 * @param zipEntryMax 打包zip的最大文件数(真实下载的文件)
	 * @param zipSizeMax 打包zip的最大字节数
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param helper ZipDownloaderHelper
	 * @param resultHandler ResultHandler
	 */
	public ZipDownloader(String zipName, List<String> uriList, String destPath, 
			int zipEntryMax, long zipSizeMax, boolean isDelSrc, 
			ZipDownloaderHelper helper, ResultHandler resultHandler) {
		this(zipName, uriList, destPath, zipEntryMax, zipSizeMax, isDelSrc, helper);
		this.resultHandler = resultHandler;
	}
	
	/**
	 * @param zipName 打包zip的名称(不带后缀)
	 * @param uriList 资源uri列表
	 * @param destPath 下载目的路径
	 * @param zipEntryMax 打包zip的最大文件数(真实下载的文件)
	 * @param zipSizeMax 打包zip的最大字节数
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param helper ZipDownloaderHelper
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public ZipDownloader(String zipName, List<String> uriList, String destPath, 
			int zipEntryMax, long zipSizeMax, boolean isDelSrc, 
			ZipDownloaderHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(zipName, uriList, destPath, zipEntryMax, zipSizeMax, isDelSrc, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}
	
	@Override
	protected boolean onStart() throws Exception {
		if(uriList.isEmpty()){
			log.warn("uriList cann't be empty."); 
			return false;
		}
		
		File dest = new File(destPath);
		if(!dest.exists() && !dest.mkdirs()){
			log.error("directory create failed: " + dest); 
			return false;
		}
		
		this.downloadPath = System.getProperty("cache.download")
				+ File.separator + name + File.separator + zipName;
		File file = new File(downloadPath);
		if(!file.exists() && !file.mkdirs()){
			log.error("directory create failed: " + downloadPath); 
			return false;
		}
		
		this.downloadZip = new File(downloadPath + File.separator + zipName + ".zip");
		return true;
	}

	/**
	 * 1.打开downloadZip的输出流(如果downloadZip已经存在则直接打开，否则新建);
	 * 2.挨个写入下载的文件流，如果压缩数或压缩文件大小大于阈值则重新命名，否则继续;
	 */
	@Override
	protected final boolean exec() throws Exception {
		return indexDownloadZip(downloadZip.exists(), false) && downloadIntoZip() && indexDownloadZip(false, true);
	}
	
	@Override
	protected boolean onComplete() throws Exception {
		File tempDir = new File(downloadPath);
		File[] files = tempDir.listFiles();
		if(ArrayUtils.isEmpty(files)){
			return true;
		}
		for(File f : files){
			if(!f.renameTo(new File(destPath + File.separator + f.getName()))){
				log.error("file move failed: " + f.getName());
				return false;
			}
		}
		if(!tempDir.delete()){
			log.error("directory delete failed: " + downloadPath);
			return false;
		}
		return true;
	}
	
	private boolean downloadIntoZip() throws Exception {
		ZipOutputStream zipOutStream = null;
		boolean isStreamClosed = true;
		try{
			for(String uri : uriList){ 
				if(isStreamClosed){
					zipOutStream = new ZipOutputStream(
							new CheckedOutputStream(new FileOutputStream(downloadZip), new CRC32()));
					isStreamClosed = false; 
				}
				long sTime = System.currentTimeMillis();
				String name = helper.getSourceName(uri);
				if(entrySet.contains(name)){
					log.warn("ignore downloaded file: " + name); 
					continue;
				}
				entrySet.add(name); 
				String size = numFormat.format(helper.zipEntry(name, uri, zipOutStream) / 1024.0);
				log.info("finish download[" + name + "(" + size + "KB)], cost=" + (System.currentTimeMillis() - sTime) + "ms");
				if(entrySet.size() >= zipEntryMax || downloadZip.length() >= zipSizeMax){
					//流管道关闭，如果继续写文件需要重新打开
					IoUtil.close(zipOutStream);
					isStreamClosed = true; 
					if(!indexDownloadZip(false, false)){
						return false;
					}
				}
			}
		}finally{
			IoUtil.close(zipOutStream);
		}
		return true;
	}
	
	/**
	 * 达到阈值的downloadZip编入序列，即重命名成指定名称
	 * @param isRetry
	 * @param isLast
	 * @return
	 * @throws Exception
	 */
	private boolean indexDownloadZip(boolean isRetry, boolean isLast) throws Exception{ 
		if(!ZipUtil.valid(downloadZip)){ 
			if(downloadZip.exists() && !downloadZip.delete()){
				log.error(downloadZip.getName() + "was damaged, and delete failed."); 
				return false;
			}
			entrySet = new HashSet<String>();
			return true;
		}

		if(isRetry){
			entrySet = getZipEntryNames(downloadZip);
		}
		if(!isLast && entrySet.size() < zipEntryMax && downloadZip.length() < zipSizeMax){
			return true;
		}

		if(isDelSrc){
			for(String entryName : entrySet){
				for(String uri : uriList){ 
					String uriName = helper.getSourceName(uri); 
					if(entryName.equals(uriName)){
						int code = helper.delete(uri);
						if(code < 200 || code > 207){
							log.error("delete src file failed: " + entryName); 
							return false;
						}
						break;
					}
				}
			}
		}

		joinOtherFiles(downloadZip); 

		String destName = getNextName(getCurrentIndex() + 1, entrySet.size());
		File destFile = new File(downloadPath + File.separator + destName);
		if(!downloadZip.renameTo(destFile)){ 
			if(!isLast){
				//继续下载下一个文件，然后再次尝试
				return true;
			}else{
				//最后一次命名失败则直接结束，交给下次任务补偿
				log.error("index zip failed: " + destName); 
				return false;
			}
		}
		index++;
		entrySet.clear();
		
		String size = numFormat.format(destFile.length() / 1024.0);
		log.info("index zip: " + destName + "(" + size + "KB)");
		return true;
	}	
	
	/**
	 * 获取下一个命名downloadZip的序列名（默认:zipName_index_entrySize.zip）
	 * @param index downloadZip命名序号
	 * @param entrySize downloadZip真实下载的文件数
	 * @return next name
	 */
	protected String getNextName(int index, int entrySize){
		StringBuilder builder = new StringBuilder(); 
		builder.append(sourceUri).append("_");
		builder.append(index).append("_").append(entrySize).append(".zip");
		return builder.toString();
	}

	/**
	 * 获取当前已有zip的最大序号
	 * @return max index of exist zip
	 */
	protected int getCurrentIndex(){
		if(index > 0){
			return index;
		}

		String[] array = new File(downloadPath).list();
		if(ArrayUtils.isEmpty(array)){
			return index;
		}
		for(String name : array){
			if(!name.startsWith(sourceUri) || name.equals(downloadZip.getName())){
				continue;
			}
			try{
				String n = name.substring(0,name.lastIndexOf("_"));
				n = n.substring(n.lastIndexOf("_") + 1,n.length());
				int i = Integer.parseInt(n);
				if(i > index){
					index = i;
				}
			}catch(Exception e){

			}
		}
		return index;
	} 

	/**
	 * 获取下载的downloadZip中的真实下载的文件名称集合
	 * @param downloadZip zip
	 * @return list of real download file name
	 * @throws Exception Exception
	 */
	protected Set<String> getZipEntryNames(File downloadZip) throws Exception {
		return ZipUtil.getEntrySet(downloadZip);
	}

	/**
	 * 自定义添加其他文件，由于上下文中考虑了失败补偿问题，建议添加文件前先检查删除已有的同名entry
	 * @param tempZipFile tempZipFile
	 * @throws IOException IOException
	 */
	protected void joinOtherFiles(File tempZipFile) throws IOException{

	}

}
