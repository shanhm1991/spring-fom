package com.fom.context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.fom.util.IoUtils;
import com.fom.util.exception.WarnException;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 * @param <E>
 */
public class HdfsZipDownloader<E extends HdfsZipDownloaderConfig> extends HdfsDownloader<E> {

	//当前已有的压缩文件最后一个序号
	protected int index;

	//压缩文件数
	protected int contents; 

	protected String subTempPath;

	private final String temp_zip;

	protected HdfsZipDownloader(String name, String path) {
		super(name, path);
		this.temp_zip = srcName + ".temp.zip";
		if(StringUtils.isBlank(config.tempPath)){
			this.subTempPath = config.destPath + File.separator + srcName;
		}else{
			this.subTempPath = config.tempPath + File.separator + srcName;
		}
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
	 * 1.将已有的temp.zip编入序列
	 * 2.将源目录中文件逐个写入temp.zip(标记文件除外)
	 * 3.当写入文件达到指定个数后，重复步骤1
	 * 4.源目录文件下载完毕，最后一次将temp.zip编入序列，如果失败就异常停止
	 */
	private void zipDownload(FileStatus[] srcFiles) throws Exception { 
		zipIndexAndGetNext(true, config); 
		boolean isStreamClosed = true;
		boolean hasZipOtherFile = false;
		ZipOutputStream zipOutStream = null;
		try{
			for(FileStatus status : srcFiles){
				if(isStreamClosed){
					File tempZip = new File(subTempPath + File.separator + temp_zip);
					zipOutStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(tempZip), new CRC32()));
					isStreamClosed = false; 
					if(!hasZipOtherFile){
						zipOtherFile(zipOutStream);
						hasZipOtherFile = true;
					}
				}

				log.info("下载文件(" + (status.getLen() / 1024) + "KB)：" + status.getPath()); 
				BufferedInputStream buffer = null;
				try{
					buffer = new BufferedInputStream(config.fs.open(status.getPath()));
					ZipEntry zipEntry = new ZipEntry(status.getPath().getName());
					zipOutStream.putNextEntry(zipEntry);
					contents++;
					int count;
					int BUFFER = 8192;
					byte[] data = new byte[BUFFER];
					while((count=buffer.read(data, 0, BUFFER))!=-1){
						zipOutStream.write(data, 0, count);
					}
				}finally{
					IoUtils.close(buffer);
				}

				if(contents >= config.getZipContent()){
					IoUtils.close(zipOutStream);
					//流管道关闭，如果继续写文件需要重新打开
					isStreamClosed = true;
					if(zipIndexAndGetNext(false, config)){
						contents = 0;
						hasZipOtherFile = false;
					}else{
						log.warn(temp_zip + "编入序列失败，继续下载"); 
					}
				}
			}
		}finally{
			IoUtils.close(zipOutStream);
		}

		//最后一个文件编入序列失败，线程自己没有机会再尝试了，只能结束自己，交给下一个线程来完成
		if(!zipIndexAndGetNext(false, config)){
			throw new WarnException(temp_zip + "编入序列失败,停止下载"); 
		}
	}

	/**
	 * 检查temp.zip
	 * 如果存在，并且没有损坏: 将远程目录中与temp中同名的文件删除 ，然后将temp编入序列
	 * 如果存在，但是是损坏的: 执行删除
	 * 如果不存在: 直接返回ture
	 * 
	 * 过程中如果发生异常或者操作失败，就抛出异常使线程结束
	 * 
	 * @throws ZipException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked") 
	private boolean zipIndexAndGetNext(boolean retry, final E config) throws Exception{ 
		File tempZip = new File(subTempPath + File.separator + temp_zip);
		if(!tempZip.exists()){
			return true;
		}

		ZipFile zip = new ZipFile(tempZip);
		if(zip.isValidZipFile()){
			if(config.delSrc){
				List<FileStatus> srcFiles = Arrays.asList(config.fs.listStatus(new Path(srcPath)));
				Iterator<FileHeader> headersIte = zip.getFileHeaders().iterator();
				while(headersIte.hasNext()){
					FileHeader header = headersIte.next();
					for(FileStatus status : srcFiles){
						if(header.getFileName().equals(status.getPath().getName())){
							log.info("删除源文件：" + status.getPath());
							if(!config.fs.delete(status.getPath(), true)){
								throw new WarnException("删除源文件失败：" + status.getPath()); 
							}
							break;
						}
					}
				}
			}

			if(retry){
				adjustRetryContents(zip.getFileHeaders().size());
			}
			File indexFile = nextZipFile(subTempPath, config);
			log.info(temp_zip + "编入序列：" + indexFile.getName());
			return tempZip.renameTo(indexFile); 
		}else{
			//zip文件损坏，可能之前下载线程在压缩文件过程中意外被结束导致zip文件损坏，应该删除重新下载
			if(!tempZip.delete()){
				throw new WarnException("删除损坏的临时zip失败"); 
			}
			log.warn("删除损坏的临时zip"); 
			return true;
		}
	}	

	protected File nextZipFile(String path, final E config){
		StringBuilder builder = new StringBuilder(path).append(File.separator); 
		builder.append(srcName).append("_");
		builder.append(getLastZipIndex() + 1).append("_").append(contents).append(".zip");
		return new File(builder.toString());
	}

	private int getLastZipIndex(){
		int max = 0;
		String[] array = new File(subTempPath).list();
		if(ArrayUtils.isEmpty(array)){
			return max;
		}
		for(String name : array){
			String n = name.substring(0,name.lastIndexOf("_"));
			n = n.substring(n.lastIndexOf("_") + 1,n.length());
			try{
				int i = Integer.parseInt(n);
				if(i > max){
					max = i;
				}
			}catch(Exception e){

			}
		}
		return max;
	}

	protected void adjustRetryContents(int zipContents){
		contents = zipContents;
	}

	protected void zipOtherFile(ZipOutputStream zipOutStream) throws IOException{

	}

	@Override
	protected void move(final E config) throws WarnException{ 
		File tempDir = new File(subTempPath);
		File[] files = tempDir.listFiles();
		if(ArrayUtils.isEmpty(files)){
			return;
		}
		for(File file : files){
			if(!file.renameTo(new File(config.destPath + File.separator + file.getName()))){
				throw new WarnException("文件移动失败:" + file.getName());
			}
		}
	}

	@Override
	protected void onComplete(final E config) throws Exception{ 
		//先删临时目录  后删源目录
		File temp = new File(subTempPath);
		if(temp.exists() && !temp.delete()){
			throw new WarnException("删除目录失败:" + subTempPath); 
		}

		if(config.delSrc && !config.fs.delete(new Path(srcPath), true)){
			throw new WarnException("删除源目录失败."); 
		}
	}
}
