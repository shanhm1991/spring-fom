package com.fom.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.fom.util.FileUtil;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;
import com.fom.util.exception.WarnException;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class HdfsZipDownloader<E extends HdfsZipDownloaderConfig> extends HdfsDownloader<E> {

	//已编入的序列的zip文件的最大序号
	protected int index;

	//tempZip中已压缩的有效数据文件数
	protected int entryContents; 
	
	//tempZip中所有entry的名字集合
	private Set<String> entrySet;

	protected String subTempPath;

	protected File subTempDir;

	protected final String tempZipName;

	protected final File tempZip;

	protected HdfsZipDownloader(String name, String path) {
		super(name, path);
		this.tempZipName = srcName + ".temp.zip";

		this.subTempPath = config.destPath;
		if(config.withTemp){
			subTempPath = config.tempPath;
		}
		subTempPath = subTempPath + File.separator + srcName;

		subTempDir = new File(subTempPath);
		if(!subTempDir.exists() && !subTempDir.mkdirs()){
			throw new RuntimeException("创建临时目录失败.");
		}
		tempZip = new File(subTempPath + File.separator + tempZipName);
	}

	@Override
	protected void download(final E config) throws Exception { 
		FileStatus[] statusArray = config.fs.listStatus(new Path(srcPath), new PathFilter(){
			@Override
			public boolean accept(Path path) {
				if(StringUtils.isBlank(config.signalFile)){
					return true;
				}
				return ! config.signalFile.equals(path.getName());
			}
		}); 

		if(ArrayUtils.isEmpty(statusArray)){
			return;
		}

		zipDownload(statusArray);
	}

	/**
	 * 1.打开tempZip的输出流(如果tempZip已经存在则直接打开，否则新建);
	 * 2.挨个写入下载的文件流，如果压缩数或压缩文件大小大于阈值则重新命名，否则继续;
	 * 
	 * @param srcFiles
	 * @throws Exception
	 */
	private void zipDownload(FileStatus[] srcFiles) throws Exception { 
		boolean isRetry = tempZip.exists();
		ZipOutputStream zipOutStream = null;
		boolean isStreamClosed = true;

		indexTempZip(isRetry, false, config); 
		try{
			for(FileStatus status : srcFiles){
				if(isStreamClosed){
					zipOutStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(tempZip), new CRC32()));
					isStreamClosed = false; 
				}
				long sTime = System.currentTimeMillis();
				String name = status.getPath().getName();
				String size = numFormat.format(status.getLen() / 1024.0);
				if(entrySet.contains(name)){
					log.warn("忽略重复下载的文件:" + name); 
				}
				ZipUtil.zipEntry(name, config.fs.open(status.getPath()), zipOutStream);
				entrySet.add(name);
				log.info("下载文件结束:" 
						+ name + "(" + size + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");

				if(++entryContents >= config.entryMax || tempZip.length() >= config.sizeMax){
					//流管道关闭，如果继续写文件需要重新打开
					IoUtil.close(zipOutStream);
					isStreamClosed = true; 
					indexTempZip(false, false, config);
				}
			}
		}finally{
			IoUtil.close(zipOutStream);
		}
		indexTempZip(false, true, config);
	}

	/**
	 * 将entry个数达到阈值的tempZip编入序列，即重命名成指定名称
	 * @param isRetry
	 * @param isLast
	 * @param config
	 * @return
	 * @throws Exception
	 */
	private void indexTempZip(boolean isRetry, boolean isLast, final E config) throws Exception{ 
		if(!tempZip.exists()){
			return;
		}

		if(ZipUtil.validZip(tempZip)){ 
			if(!tempZip.delete()){
				throw new WarnException(tempZipName + "已经损坏, 删除失败."); 
			}
			return;
		}

		entrySet = ZipUtil.getEntrySet(tempZip);
		if(isRetry){
			entryContents = getTempZipContents(entrySet);
		}
		if(!isLast && entryContents < config.entryMax && tempZip.length() < config.sizeMax){
			return;
		}

		if(config.delSrc){
			List<FileStatus> srcFiles = Arrays.asList(config.fs.listStatus(new Path(srcPath)));
			for(String name : entrySet){
				for(FileStatus status : srcFiles){
					if(name.equals(status.getPath().getName())){
						log.info("删除源文件：" + status.getPath());
						if(!config.fs.delete(status.getPath(), true)){
							throw new WarnException("删除源文件失败：" + status.getPath()); 
						}
						break;
					}
				}
			}
		}

		joinOtherFiles(tempZip); 

		String name = nextZipName(config);
		if(!tempZip.renameTo(new File(name)) && isLast){
			//最后一次编入序列失败则直接结束，交给下次任务补偿
			throw new WarnException("编入序列失败:" + name); 
		}
		
		index++;
		entrySet.clear();
		entryContents = 0;
		log.info("编入序列：" + name);
	}	

	/**
	 * 获取下一个zip的序列名
	 * @param config
	 * @return
	 */
	protected String nextZipName(final E config){
		StringBuilder builder = new StringBuilder(subTempPath).append(File.separator); 
		builder.append(srcName).append("_");
		builder.append(currentIndex() + 1).append("_").append(entryContents).append(".zip");
		return builder.toString();
	}

	/**
	 * 获取当前已有zip的最大序号
	 * @return
	 */
	protected int currentIndex(){
		if(index > 0){
			return index;
		}

		String[] array = new File(subTempPath).list();
		if(ArrayUtils.isEmpty(array)){
			return index;
		}
		for(String name : array){
			String n = name.substring(0,name.lastIndexOf("_"));
			n = n.substring(n.lastIndexOf("_") + 1,n.length());
			try{
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
		FileUtil.moveTemp(subTempPath, config.destPath, true);
	}

	@Override
	protected void onComplete(final E config) throws Exception{ 
		if(config.delSrc && !config.fs.delete(new Path(srcPath), true)){
			throw new WarnException("删除源目录失败."); 
		}
	}
}
