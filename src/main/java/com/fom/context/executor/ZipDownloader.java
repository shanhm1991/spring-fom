package com.fom.context.executor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fom.context.Executor;
import com.fom.context.exception.WarnException;
import com.fom.context.executor.helper.DownloaderHelper;
import com.fom.log.LoggerFactory;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 
 * @author shanhm1991
 *
 */
public class ZipDownloader implements Executor {
	
	protected final Logger log;
	
	protected final DecimalFormat numFormat  = new DecimalFormat("#.##");

	private static AtomicInteger num = new AtomicInteger(0);

	private List<String> uriList;

	private ZipDownloaderConfig config;

	private DownloaderHelper helper;

	private String zipName;

	private String downloadPath;

	private File downloadZip; 

	//downloadZip的命名序号
	private int index;

	//downloadZip中真实下载的文件名称集合
	private Set<String> entrySet;

	/**
	 * 
	 * @param name 模块名称
	 * @param zipName 指定下载打包zip的名称(不带后缀)
	 * @param uriList 下载资源uri列表
	 * @param config ZipDownloaderConfig
	 * @param helper DownloaderHelper
	 */
	public ZipDownloader(String name, String zipName, List<String> uriList, ZipDownloaderConfig config, DownloaderHelper helper) {
		if(StringUtils.isBlank(name) || StringUtils.isBlank(zipName) 
				|| config == null || helper == null || uriList == null) {
			throw new IllegalArgumentException();
		}
		this.log = LoggerFactory.getLogger(name);
		this.zipName = zipName;
		this.uriList = uriList;
		this.config = config;
		this.helper = helper;
		this.downloadPath = System.getProperty("download.temp")
				+ File.separator + name + File.separator + zipName + (num.incrementAndGet() / 1000000);
		this.downloadZip = new File(downloadPath + File.separator + zipName + ".zip");
	}

	/**
	 * 1.打开downloadZip的输出流(如果downloadZip已经存在则直接打开，否则新建);
	 * 2.挨个写入下载的文件流，如果压缩数或压缩文件大小大于阈值则重新命名，否则继续;
	 */
	@Override
	public void exec() throws Exception {
		if(uriList.isEmpty()){
			return;
		}
		
		File file = new File(downloadPath);
		if(!file.exists() && !file.mkdirs()){
			throw new IllegalArgumentException("下载目录创建失败:" + downloadPath); 
		}
		
		boolean isRetry = downloadZip.exists();
		ZipOutputStream zipOutStream = null;
		boolean isStreamClosed = true;

		indexDownloadZip(isRetry, false); 
		try{
			for(String uri : uriList){ 
				if(isStreamClosed){
					zipOutStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(downloadZip), new CRC32()));
					isStreamClosed = false; 
				}
				long sTime = System.currentTimeMillis();
				String name = config.getSourceName(uri);
				if(entrySet.contains(name)){
					log.warn("忽略重复下载的文件:" + name); 
					continue;
				}
				entrySet.add(name); 
				
				String size = numFormat.format(ZipUtil.zipEntry(name, helper.open(uri), zipOutStream) / 1024.0);
				log.info("下载文件结束:" + name + "(" + size + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
				if(entrySet.size() >= config.getEntryMax() || downloadZip.length() >= config.getSizeMax()){
					//流管道关闭，如果继续写文件需要重新打开
					IoUtil.close(zipOutStream);
					isStreamClosed = true; 
					indexDownloadZip(false, false);
				}
			}
		}finally{
			IoUtil.close(zipOutStream);
		}
		indexDownloadZip(false, true);
		
		//move
		File tempDir = new File(downloadPath);
		File[] files = tempDir.listFiles();
		if(ArrayUtils.isEmpty(files)){
			return;
		}
		for(File f : files){
			if(!f.renameTo(new File(config.getDestPath() + File.separator + f.getName()))){
				throw new WarnException("文件移动失败:" + f.getName());
			}
		}
		if(!tempDir.delete()){
			throw new WarnException("删除目录失败:" + downloadPath);
		}
	}

	/**
	 * 达到阈值的downloadZip编入序列，即重命名成指定名称
	 * @param isRetry
	 * @param isLast
	 * @return
	 * @throws Exception
	 */
	private void indexDownloadZip(boolean isRetry, boolean isLast) throws Exception{ 
		if(!ZipUtil.valid(downloadZip)){ 
			if(downloadZip.exists() && !downloadZip.delete()){
				throw new WarnException(downloadZip.getName() + "已经损坏, 删除失败."); 
			}
			entrySet = new HashSet<String>();
			return;
		}

		if(isRetry){
			entrySet = getZipEntryNames(downloadZip);
		}
		if(!isLast && entrySet.size() < config.getEntryMax() 
				&& downloadZip.length() < config.getSizeMax()){
			return;
		}

		if(config.isDelSrc()){
			for(String entryName : entrySet){
				for(String uri : uriList){ 
					String uriName = config.getSourceName(uri); 
					if(entryName.equals(uriName)){
						log.info("删除源文件：" + entryName);
						if(!helper.delete(uri)){
							throw new WarnException("删除源文件失败：" + entryName); 
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
				return;
			}else{
				//最后一次命名失败则直接结束，交给下次任务补偿
				throw new WarnException("命名文件失败:" + destName); 
			}
		}
		index++;
		entrySet.clear();
		
		String size = numFormat.format(destFile.length() / 1024.0);
		log.info("命名文件：" + destName + "(" + size + "KB)");
	}	

	/**
	 * 获取下一个命名downloadZip的序列名（默认:zipName_index_entrySize.zip）
	 * @param index downloadZip命名序号
	 * @param entrySize downloadZip真实下载的文件数
	 * @return
	 */
	protected String getNextName(int index, int entrySize){
		StringBuilder builder = new StringBuilder(); 
		builder.append(zipName).append("_");
		builder.append(index).append("_").append(entrySize).append(".zip");
		return builder.toString();
	}

	/**
	 * 获取当前已有zip的最大序号
	 * @return
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
			if(!name.startsWith(zipName) || name.equals(downloadZip.getName())){
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
	 * @param downloadZip
	 * @return
	 * @throws Exception
	 */
	protected Set<String> getZipEntryNames(File downloadZip) throws Exception {
		return ZipUtil.getEntrySet(downloadZip);
	}

	/**
	 * 自定义添加其他文件，由于上下文中考虑了失败补偿问题，建议添加文件前先检查删除已有的同名entry
	 * @param tempZipFile
	 * @throws IOException
	 */
	protected void joinOtherFiles(File tempZipFile) throws IOException{

	}

}
