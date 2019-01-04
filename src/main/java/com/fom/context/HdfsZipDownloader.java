package com.fom.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class HdfsZipDownloader<E extends HdfsZipDownloaderConfig> extends HdfsDownloader<E> {

	//当前已有的压缩文件最后一个序号
	protected int index;

	//压缩文件数
	protected int contents; 

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
	 * 
	 * @param srcFiles
	 * @throws Exception
	 */
	private void zipDownload(FileStatus[] srcFiles) throws Exception { 
		boolean isRetry = tempZip.exists();
		ZipOutputStream zipOutStream = null;
		boolean isStreamClosed = true;

		try{
			indexTempZip(isRetry, config); 
			
			for(FileStatus status : srcFiles){
				
				
				if(contents >= config.getZipContent()){
					IoUtil.close(zipOutStream);
					isStreamClosed = true; 
				}
				
				
				
				
				
				if(isStreamClosed){
					zipOutStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(tempZip), new CRC32()));
					isStreamClosed = false; 
				}

				long sTime = System.currentTimeMillis();
				String name = status.getPath().getName();
				ZipUtil.zipEntry(name, config.fs.open(status.getPath()), zipOutStream);
				String size = numFormat.format(status.getLen() / 1024.0);
				log.info("下载文件结束:" 
						+ name + "(" + size + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");

				if(++contents >= config.getZipContent()){
					IoUtil.close(zipOutStream);
					//流管道关闭，如果继续写文件需要重新打开
					isStreamClosed = true; 
					if(indexTempZip(false, config)){
						contents = 0;
						continue;
					}
					log.warn(tempZipName + "编入序列失败，继续下载"); 
				}
			}

			//最后一个文件编入序列失败，线程自己没有机会再尝试了，只能结束自己，交给下一个线程来完成
			if(!indexTempZip(false, config)){
				throw new WarnException(tempZipName + "编入序列失败,停止下载"); 
			}
		}finally{
			IoUtil.close(zipOutStream);
		}
	}

	/**
	 * 
	 * @param isRetry
	 * @param config
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked") 
	private boolean indexTempZip(boolean isRetry, final E config) throws Exception{ 
		if(!tempZip.exists()){
			return true;
		}

		ZipFile zip = new ZipFile(tempZip);
		if(!zip.isValidZipFile()){
			if(!tempZip.delete()){
				throw new WarnException(tempZipName + "已经损坏, 删除失败."); 
			}
			return true;
		}
		
		if(isRetry){
			contents = zip.getFileHeaders().size();
		}
		
		if(contents < config.getZipContent()){
			return true;
		}

		if(config.delSrc){
			List<FileStatus> srcFiles = Arrays.asList(config.fs.listStatus(new Path(srcPath)));
			Iterator<FileHeader> headersIte = zip.getFileHeaders().iterator();
			while(headersIte.hasNext()){
				String name = headersIte.next().getFileName();
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

		joinOtherFiles(); //TODO

		File indexZip = nextZipFile(subTempPath, config);
		log.info(tempZipName + "编入序列：" + indexZip.getName());
		return tempZip.renameTo(indexZip); 
	}	

	protected File nextZipFile(String path, final E config){
		StringBuilder builder = new StringBuilder(path).append(File.separator); 
		builder.append(srcName).append("_");
		builder.append(nextIndex()).append("_").append(contents).append(".zip");
		return new File(builder.toString());
	}

	private int nextIndex(){
		if(index > 0){
			index++;
			return index;
		}


		int index = 0;
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

	protected void joinOtherFiles() throws IOException{

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
