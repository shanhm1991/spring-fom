package com.fom.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.ArrayUtils;

import com.fom.context.exception.WarnException;
import com.fom.context.executor.Downloader;
import com.fom.context.executor.IZipDownloaderConfig;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public abstract class ZipDownloader<E extends IZipDownloaderConfig> extends Downloader<E>{

	//已编入的序列的zip文件的最大序号
	protected int index;

	//tempZip中已压缩的有效数据文件数
	protected int entryContents; 

	//tempZip中所有entry的名字集合
	protected Set<String> entrySet;

	protected String subTempPath;

	protected File subTempDir;

	protected final String tempZipName;

	protected final File tempZip;

	protected ZipDownloader(String name, String path) {
		super(name, path);
		this.tempZipName = srcName + ".temp.zip";
		this.subTempPath = config.getDestPath();
		if(config.isWithTemp()){
			subTempPath = config.getTempPath();
		}
		subTempPath = subTempPath + File.separator + srcName;

		subTempDir = new File(subTempPath);
		if(!subTempDir.exists() && !subTempDir.mkdirs()){
			throw new RuntimeException("创建临时目录失败.");
		}
		tempZip = new File(subTempPath + File.separator + tempZipName);
	}

	/**
	 * 1.打开tempZip的输出流(如果tempZip已经存在则直接打开，否则新建);
	 * 2.挨个写入下载的文件流，如果压缩数或压缩文件大小大于阈值则重新命名，否则继续;
	 */
	@Override
	protected void download(final E config) throws Exception {
		List<String> pathList = getPathList();
		if(pathList == null || pathList.isEmpty()){
			return;
		}

		boolean isRetry = tempZip.exists();
		ZipOutputStream zipOutStream = null;
		boolean isStreamClosed = true;

		renameTempZip(isRetry, false, config); 
		try{
			for(String path : pathList){
				if(isStreamClosed){
					zipOutStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(tempZip), new CRC32()));
					isStreamClosed = false; 
				}
				
				long sTime = System.currentTimeMillis();
				String name = new File(path).getName();
				String size = numFormat.format(getResourceSize(path) / 1024.0);
				if(entrySet.contains(name)){
					log.warn("忽略重复下载的文件:" + name); 
				}
				ZipUtil.zipEntry(name, getResourceInputStream(path), zipOutStream);
				entrySet.add(name);
				log.info("下载文件结束:" 
						+ name + "(" + size + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");

				if(++entryContents >= config.getEntryMax() || tempZip.length() >= config.getSizeMax()){
					//流管道关闭，如果继续写文件需要重新打开
					IoUtil.close(zipOutStream);
					isStreamClosed = true; 
					renameTempZip(false, false, config);
				}
			}
		}finally{
			IoUtil.close(zipOutStream);
		}
		renameTempZip(false, true, config);
	}

	/**
	 * 将entry个数达到阈值的tempZip编入序列，即重命名成指定名称
	 * @param isRetry
	 * @param isLast
	 * @param config
	 * @return
	 * @throws Exception
	 */
	private void renameTempZip(boolean isRetry, boolean isLast, final E config) throws Exception{ 
		if(!ZipUtil.validZip(tempZip)){ 
			if(tempZip.exists() && !tempZip.delete()){
				throw new WarnException(tempZipName + "已经损坏, 删除失败."); 
			}
			entrySet = new HashSet<String>();
			return;
		}

		if(isRetry){
			entrySet = ZipUtil.getEntrySet(tempZip);
			entryContents = getTempZipContents(entrySet);
		}
		if(!isLast && entryContents < config.getEntryMax() && tempZip.length() < config.getSizeMax()){
			return;
		}

		if(config.isDelSrc()){
			List<String> pathList = getPathList();
			for(String name : entrySet){
				for(String path : pathList){
					String pathName = new File(path).getName();
					if(name.equals(pathName)){
						log.info("删除源文件：" + name);
						if(!deletePath(path)){
							throw new WarnException("删除源文件失败：" + name); 
						}
						break;
					}
				}
			}
		}

		joinOtherFiles(tempZip); 

		String name = getNextName(config);
		if(!tempZip.renameTo(new File(subTempPath + File.separator + name))){ 
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
	protected String getNextName(final E config){
		StringBuilder builder = new StringBuilder(); 
		builder.append(srcName).append("_");
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

		String[] array = new File(subTempPath).list();
		if(ArrayUtils.isEmpty(array)){
			return index;
		}
		for(String name : array){
			if(!name.startsWith(srcName) || name.equals(tempZipName)){
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
	
	@Override
	protected void move(final E config) throws WarnException{ 
		File tempDir = new File(subTempPath);
		File[] files = tempDir.listFiles();
		if(ArrayUtils.isEmpty(files)){
			return;
		}
		for(File file : files){
			if(!file.renameTo(new File(config.getDestPath() + File.separator + file.getName()))){
				throw new WarnException("文件移动失败:" + file.getName());
			}
		}
		if(!tempDir.delete()){
			throw new WarnException("删除临时目录失败.");
		}
	}

	@Override
	protected void onComplete(final E config) throws Exception{ 
		if(config.isDelSrc() && !deletePath(srcPath)){
			throw new WarnException("删除源目录失败."); 
		}
	}
	
	/**
	 * 获取远程目录下文件的路径集合
	 * @return
	 * @throws Exception
	 */
	protected abstract List<String> getPathList() throws Exception;
	
	/**
	 * 根据路径打开远程文件的输入流
	 * @param path
	 * @return
	 * @throws Exception
	 */
	protected abstract InputStream getResourceInputStream(String path) throws Exception;
	
	/**
	 * 根据路径获取远程文件的大小
	 * @param path
	 * @return
	 * @throws Exception
	 */
	protected abstract long getResourceSize(String path) throws Exception;
	
	/**
	 * 根据路径删除远程文件
	 * @param path
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean deletePath(String path) throws Exception;
}
