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

import com.fom.context.Executor;
import com.fom.context.helper.ZipDownloaderHelper;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 根据sourceUri列表下载并打包成zip文件的执行器
 * 
 * @author shanhm1991
 *
 */
public class ZipDownloader extends Executor {
	
	private static final AtomicInteger num = new AtomicInteger(0);
	
	private final DecimalFormat numFormat  = new DecimalFormat("#.##");
	
	protected final ZipDownloaderHelper helper;

	private List<String> uriList;
	
	private final String destPath;
	
	private final int zipEntryMax;
	
	private final long zipSizeMax;
	
	private final boolean isDelSrc;

	private final String downloadPath;

	private final File downloadZip; 

	//downloadZip的命名序号
	private int index;

	//downloadZip中真实下载的文件名称集合
	private Set<String> entrySet;

	/**
	 * @param name 模块名称
	 * @param zipName 打包zip的名称(不带后缀)
	 * @param uriList 资源uri列表
	 * @param destPath 下载目的路径
	 * @param zipEntryMax 打包zip的最大文件数(真实下载的文件)
	 * @param zipSizeMax 打包zip的最大字节数
	 * @param isDelSrc 下载结束是否删除资源文件
	 * @param helper ZipDownloaderHelper
	 */
	public ZipDownloader(String name, String zipName, List<String> uriList, String destPath, 
			int zipEntryMax, long zipSizeMax, boolean isDelSrc, ZipDownloaderHelper helper) {
		super(name, zipName);
		if(StringUtils.isBlank(name) || StringUtils.isBlank(zipName) 
				 || helper == null || uriList == null) {
			throw new IllegalArgumentException();
		}
		this.helper = helper;
		this.uriList = uriList;
		this.destPath = destPath;
		this.zipEntryMax = zipEntryMax;
		this.zipSizeMax = zipSizeMax;
		this.isDelSrc = isDelSrc;
		this.downloadPath = System.getProperty("download.temp")
				+ File.separator + name + File.separator + zipName + (num.incrementAndGet() / 1000000);
		this.downloadZip = new File(downloadPath + File.separator + zipName + ".zip");
	}
	
	@Override
	protected final boolean onStart() throws Exception {
		if(uriList.isEmpty()){
			log.warn("资源uri列表为空"); 
			return false;
		}
		File file = new File(downloadPath);
		if(!file.exists() && !file.mkdirs()){
			log.error("下载目录创建失败:" + downloadPath); 
			return false;
		}
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
	protected final boolean onComplete() throws Exception {
		File tempDir = new File(downloadPath);
		File[] files = tempDir.listFiles();
		if(ArrayUtils.isEmpty(files)){
			return true;
		}
		for(File f : files){
			if(!f.renameTo(new File(destPath + File.separator + f.getName()))){
				log.error("文件移动失败:" + f.getName());
				return false;
			}
		}
		if(!tempDir.delete()){
			log.error("删除目录失败:" + downloadPath);
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
					log.warn("忽略重复下载的文件:" + name); 
					continue;
				}
				entrySet.add(name); 
				
				String size = numFormat.format(ZipUtil.zipEntry(name, helper.open(uri), zipOutStream) / 1024.0);
				log.info("下载文件结束:" + name + "(" + size + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
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
				log.error(downloadZip.getName() + "已经损坏, 删除失败."); 
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
						if(!helper.delete(uri)){
							log.error("删除源文件失败：" + entryName); 
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
				log.error("命名文件失败:" + destName); 
				return false;
			}
		}
		index++;
		entrySet.clear();
		
		String size = numFormat.format(destFile.length() / 1024.0);
		log.info("命名文件：" + destName + "(" + size + "KB)");
		return true;
	}	
	
	/**
	 * 获取下一个命名downloadZip的序列名（默认:zipName_index_entrySize.zip）
	 * @param index downloadZip命名序号
	 * @param entrySize downloadZip真实下载的文件数
	 * @return
	 */
	protected String getNextName(int index, int entrySize){
		StringBuilder builder = new StringBuilder(); 
		builder.append(sourceName).append("_");
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
			if(!name.startsWith(sourceName) || name.equals(downloadZip.getName())){
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
