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
import org.apache.log4j.Logger;

import com.fom.context.exception.WarnException;
import com.fom.context.executor.helper.downloader.DownloaderHelper;
import com.fom.log.LoggerFactory;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 
 * @author shanhm1991
 * @date 2019年1月21日
 *
 */
public class ZipDownloader implements Executor {

	protected final Logger log;
	
	protected final String name;
	
	protected final List<String> urlList;
	
	protected final DownloaderHelper helper;

	/**
	 * zip文件基础名称(不带后缀)
	 */
	protected final String zipName;

	/**
	 * zip最大压缩文件个数
	 */
	protected final int entryMax;

	/**
	 * zip文件最大字节数
	 */
	protected final long sizeMax;


	protected final String tempPath;

	protected final String destPath;

	protected final File tempZip;

	protected final boolean isDelSrc;

	//已编入的序列的zip文件的最大序号
	protected int index;

	//tempZip中已压缩的有效数据文件数
	protected int entryContents; 

	//tempZip中所有entry的名字集合
	protected Set<String> entrySet;

	DecimalFormat numFormat  = new DecimalFormat("#.##");

	/**
	 * 
	 * @param name
	 * @param urlList
	 * @param config
	 * @param helper
	 */
	public ZipDownloader(String name, List<String> urlList, IZipDownloaderConfig config, DownloaderHelper helper) {
		if(StringUtils.isBlank(name) || config == null || helper == null || urlList == null) {
			throw new IllegalArgumentException();
		}
		this.helper = helper;
		this.urlList = urlList;
		this.name = name;
		this.log = LoggerFactory.getLogger(name);
		
		this.zipName = config.getFileName();
		this.entryMax = config.getEntryMax();
		this.sizeMax = config.getSizeMax();
		this.destPath = config.getDestPath();
		this.isDelSrc = config.isDelSrc();
		if(config.isWithTemp()){
			this.tempPath = System.getProperty("download.temp") + File.separator + name + File.separator + zipName;
		}else{
			this.tempPath = destPath + File.separator + zipName;
		}
		File path = new File(tempPath);
		if(!path.exists() && !path.mkdirs()){
			throw new IllegalArgumentException("无法创建临时目录:" + tempPath); 
		}
		tempZip = new File(tempPath + File.separator + zipName + ".temp.zip");
	}

	/**
	 * 1.打开tempZip的输出流(如果tempZip已经存在则直接打开，否则新建);
	 * 2.挨个写入下载的文件流，如果压缩数或压缩文件大小大于阈值则重新命名，否则继续;
	 */
	@Override
	public void exec() throws Exception {
		if(urlList.isEmpty()){
			return;
		}
		boolean isRetry = tempZip.exists();
		ZipOutputStream zipOutStream = null;
		boolean isStreamClosed = true;

		renameTempZip(isRetry, false); 
		try{
			for(String path : urlList){
				if(isStreamClosed){
					zipOutStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(tempZip), new CRC32()));
					isStreamClosed = false; 
				}
				long sTime = System.currentTimeMillis();
				String name = new File(path).getName();
				if(entrySet.contains(name)){
					log.warn("忽略重复下载的文件:" + name); 
					continue;
				}
				long size = ZipUtil.zipEntry(name, helper.open(path), zipOutStream);
				entrySet.add(name); //TODO
				log.info("下载文件结束:" + name + "(" 
						+ numFormat.format(size / 1024.0) + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
				if(++entryContents >= entryMax || tempZip.length() >= sizeMax){
					//流管道关闭，如果继续写文件需要重新打开
					IoUtil.close(zipOutStream);
					isStreamClosed = true; 
					renameTempZip(false, false);
				}
			}
		}finally{
			IoUtil.close(zipOutStream);
		}
		renameTempZip(false, true);
	}

	/**
	 * 将entry个数达到阈值的tempZip编入序列，即重命名成指定名称
	 * @param isRetry
	 * @param isLast
	 * @return
	 * @throws Exception
	 */
	private void renameTempZip(boolean isRetry, boolean isLast) throws Exception{ 
		if(!ZipUtil.validZip(tempZip)){ 
			if(tempZip.exists() && !tempZip.delete()){
				throw new WarnException(tempZip.getName() + "已经损坏, 删除失败."); 
			}
			entrySet = new HashSet<String>();
			return;
		}

		if(isRetry){
			entrySet = ZipUtil.getEntrySet(tempZip);
			entryContents = getTempZipContents(entrySet);
		}
		if(!isLast && entryContents < entryMax && tempZip.length() < sizeMax){
			return;
		}

		if(isDelSrc){
			List<String> pathList = urlList; //TODO pathlist维护
			for(String name : entrySet){
				for(String path : pathList){
					String pathName = new File(path).getName();
					if(name.equals(pathName)){
						log.info("删除源文件：" + name);
						if(!helper.delete(path)){
							throw new WarnException("删除源文件失败：" + name); 
						}
						break;
					}
				}
			}
		}

		joinOtherFiles(tempZip); 

		String name = getNextName();
		if(!tempZip.renameTo(new File(tempPath + File.separator + name))){ 
			if(!isLast){
				//继续下载下一个文件，然后再次尝试
				return;
			}else{
				//最后一次命名失败则直接结束，交给下次任务补偿
				throw new WarnException("命名文件失败:" + name); 
			}
		}
		index++;
		entrySet.clear();
		entryContents = 0;
		log.info("命名文件：" + name);
	}	

	/**
	 * 获取下一个zip的序列名
	 * @param config
	 * @return
	 */
	protected String getNextName(){
		StringBuilder builder = new StringBuilder(); 
		builder.append(zipName).append("_");
		builder.append(getCurrentIndex() + 1).append("_").append(entryContents).append(".zip");
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

		String[] array = new File(tempPath).list();
		if(ArrayUtils.isEmpty(array)){
			return index;
		}
		for(String name : array){
			if(!name.startsWith(zipName) || name.equals(tempZip.getName())){
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
	 * 获取zip中的有效文件数
	 * @param entrySet
	 * @return
	 * @throws Exception
	 */
	protected int getTempZipContents(Set<String> entrySet) throws Exception {
		return entrySet.size();
	}

	/**
	 * 自定义添加其他文件，由于上下文考虑了失败补偿问题，建议添加文件前先检查删除已有的同名entry
	 * @param tempZipFile
	 * @throws IOException
	 */
	protected void joinOtherFiles(File tempZipFile) throws IOException{

	}

	protected void move() throws WarnException{ 
		File tempDir = new File(tempPath);
		File[] files = tempDir.listFiles();
		if(ArrayUtils.isEmpty(files)){
			return;
		}
		for(File file : files){
			if(!file.renameTo(new File(destPath + File.separator + file.getName()))){
				throw new WarnException("文件移动失败:" + file.getName());
			}
		}
		if(!tempDir.delete()){
			throw new WarnException("删除临时目录失败.");
		}
	}

	//	@Override
	//	protected void onComplete(final E config) throws Exception{ 
	//		if(config.isDelSrc() && !deletePath(srcPath)){
	//			throw new WarnException("删除源目录失败."); 
	//		}
	//	}

//	/**
//	 * 根据路径打开远程文件的输入流
//	 * @param url
//	 * @return
//	 * @throws Exception
//	 */
//	protected abstract InputStream open(String url) throws Exception;
//
//	/**
//	 * 根据路径删除远程文件
//	 * @param url
//	 * @return
//	 * @throws Exception
//	 */
//	protected abstract boolean delete(String url) throws Exception;

}
