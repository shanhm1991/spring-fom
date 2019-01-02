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

import com.fom.util.FileUtil;
import com.fom.util.IoUtil;
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

	private final String tempZipName;

	protected HdfsZipDownloader(String name, String path) {
		super(name, path);
		this.tempZipName = srcName + ".temp.zip";
		this.subTempPath = config.destPath;
		if(config.withTemp){
			subTempPath = config.tempPath;
		}
		subTempPath = subTempPath + File.separator + srcName;
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
					File tempZip = new File(subTempPath + File.separator + tempZipName);
					zipOutStream = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(tempZip), new CRC32()));
					isStreamClosed = false; 
					if(!hasZipOtherFile){
						zipOtherFile(zipOutStream);
						hasZipOtherFile = true;
					}
				}

				long sTime = System.currentTimeMillis();
				String size = numFormat.format(status.getLen() / 1024.0);
				BufferedInputStream buffer = null;
				try{
					buffer = new BufferedInputStream(config.fs.open(status.getPath()));
					ZipEntry zipEntry = new ZipEntry(status.getPath().getName());
					zipOutStream.putNextEntry(zipEntry);
					int count;
					int BUFFER = 8192;
					byte[] data = new byte[BUFFER];
					while((count=buffer.read(data, 0, BUFFER))!=-1){
						zipOutStream.write(data, 0, count);
					}
					contents++;
				}finally{
					IoUtil.close(buffer);
				}
				log.info("下载文件结束:" + status.getPath().getName() 
						+ "(" + size + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");

				if(contents >= config.getZipContent()){
					IoUtil.close(zipOutStream);
					//流管道关闭，如果继续写文件需要重新打开
					isStreamClosed = true;
					if(zipIndexAndGetNext(false, config)){
						contents = 0;
						hasZipOtherFile = false;
					}else{
						log.warn(tempZipName + "编入序列失败，继续下载"); 
					}
				}
			}
		}finally{
			IoUtil.close(zipOutStream);
		}

		//最后一个文件编入序列失败，线程自己没有机会再尝试了，只能结束自己，交给下一个线程来完成
		if(!zipIndexAndGetNext(false, config)){
			throw new WarnException(tempZipName + "编入序列失败,停止下载"); 
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
	private boolean zipIndexAndGetNext(boolean isRetry, final E config) throws Exception{ 
		File tempFile = new File(subTempPath + File.separator + tempZipName);
		if(!tempFile.exists()){
			return true;
		}

		ZipFile tempZip = new ZipFile(tempFile);
		if(!tempZip.isValidZipFile()){
			if(!tempFile.delete()){
				throw new WarnException(tempZipName + "已经损坏, 删除失败."); 
			}
			return true;
		}

		if(config.delSrc){
			List<FileStatus> srcFiles = Arrays.asList(config.fs.listStatus(new Path(srcPath)));
			Iterator<FileHeader> headersIte = tempZip.getFileHeaders().iterator();
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

		if(isRetry){
			contents = tempZip.getFileHeaders().size();
		}
		File indexFile = nextZipFile(subTempPath, config);
		log.info(tempZipName + "编入序列：" + indexFile.getName());
		return tempFile.renameTo(indexFile); 
	}	

	protected File nextZipFile(String path, final E config){
		StringBuilder builder = new StringBuilder(path).append(File.separator); 
		builder.append(srcName).append("_");
		builder.append(getLastZipIndex() + 1).append("_").append(contents).append(".zip");
		return new File(builder.toString());
	}

	private int getLastZipIndex(){
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

	protected void zipOtherFile(ZipOutputStream zipOutStream) throws IOException{

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
