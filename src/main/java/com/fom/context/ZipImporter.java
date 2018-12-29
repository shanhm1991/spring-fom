package com.fom.context;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;

import com.fom.util.ZipUtil;
import com.fom.util.exception.WarnException;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 * @param <V>
 */
public abstract class ZipImporter<E extends ZipImporterConfig,V> extends Importer<E,V> {

	protected final File unzipDir;

	private boolean removeDirectly;

	private List<String> nameList;

	protected ZipImporter(String name, String path) {
		super(name, path);
		this.unzipDir = new File(System.getProperty("import.progress")
				+ File.separator + name + File.separator + srcName);
	}

	/**
	 * 1.解压到缓存目录，并校验解压结果是否合法
	 * 2.创建对应的处理日志文件
	 * 3.挨个处理并删除文件
	 * 4.清空并删除解压目录
	 * 5.删除源文件
	 * 6.删除日志文件
	 * 上述任何步骤返回失败或出现异常则结束任务
	 */
	void execute() throws Exception {
		if(logFile.exists()){
			log.warn("继续处理任务遗留文件."); 
			//上次任务在第5步失败
			if(!unzipDir.exists()){ 
				removeDirectly = true;
				return;
			}
			//上次任务在第3步或者第4步失败
			String[] nameArray = unzipDir.list();
			//失败在第4步
			if(nameArray == null || nameArray.length == 0){ 
				removeDirectly = true;
				return;
			}
			nameList = Arrays.asList(nameArray);
			//失败在第4步
			if(!matchContents() || !validContents(config, nameList)){
				removeDirectly = true;
				return;
			}

			//处理未完成文件
			processFailedFile();
		} else {
			//上次任务在第1步或者第2步失败
			if(unzipDir.exists()){
				File[] fileArray = unzipDir.listFiles();
				if(!ArrayUtils.isEmpty(fileArray)){
					log.warn("清空未正确解压的文件目录");
					for(File file : fileArray){
						if(!file.delete()){
							throw new WarnException("清除文件失败:" + file.getName()); 
						}
					}
				}
			}else{
				//首次任务处理
				if(!unzipDir.mkdirs()){
					throw new WarnException("创建解压目录失败: " + unzipDir.getName());
				}
			}

			try{
				long cost = ZipUtil.unZip(srcFile, unzipDir);
				log.info("解压结束(" + numFormat.format(srcSize) + "KB), 耗时=" + cost + "ms");
			}catch(ZipException e){
				log.error("解压失败", e); 
				removeDirectly = true;
				return; 
			}

			String[] nameArray = unzipDir.list();
			if(nameArray == null || nameArray.length == 0){ 
				removeDirectly = true;
				return;
			}

			nameList = Arrays.asList(nameArray);
			if(!matchContents() || !validContents(config, nameList)){
				removeDirectly = true;
				return;
			}

			if(!logFile.createNewFile()){
				throw new WarnException("创建日志文件失败.");
			}
		}

		processFiles();
	}

	protected boolean validContents(E config, List<String> nameList) {
		return true;
	}

	private boolean matchContents(){
		List<String> list = new LinkedList<>();
		for(String name : nameList){
			if(config.matchZipContent(name)){
				list.add(name);
			}
		}
		nameList = list;
		return nameList.size() > 0;
	}

	private void processFailedFile() throws Exception {  
		List<String> lines = FileUtils.readLines(logFile);
		//刚创建完日志文件,线程结束
		if(lines.isEmpty()){
			return;
		}

		String name = lines.get(0); 
		File file = new File(unzipDir + File.separator + name);
		if(!file.exists()){
			log.warn("未找到任务遗留文件:" + name);
			return;
		}

		long sTime = System.currentTimeMillis();
		double size = file.length() / 1024.0;
		int lineIndex = 0;
		try{
			lineIndex = Integer.valueOf(lines.get(1));
			log.info("获取文件处理进度[" + name + "]:" + lineIndex); 
		}catch(Exception e){
			log.warn("获取文件处理进度失败,将从第0行开始处理:" + name);
		}

		readFile(file, lineIndex);
		nameList.remove(name);
		log.info("处理文件结束[" + name + "(" 
				+ numFormat.format(size) + "KB)], 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		if(!file.delete()){
			throw new WarnException("删除文件失败:" + name); 
		}
	}

	private void processFiles() throws Exception {
		Iterator<String> it = nameList.iterator();
		while(it.hasNext()){
			String name = it.next();
			long sTime = System.currentTimeMillis();
			File file = new File(unzipDir + File.separator + name);
			double size = file.length() / 1024.0;
			readFile(file, 0);
			log.info("处理文件结束[" + name + "(" 
					+ numFormat.format(size) + "KB)], 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
			
			it.remove();
			if(!file.delete()){
				throw new WarnException("删除文件失败:" + file.getName()); 
			}
		}
	}

	void onFinally() {
		if(!removeDirectly && nameList.size() > 0){
			log.warn("遗留任务文件, 等待下次处理."); 
			return;
		}

		if(unzipDir.exists()){ 
			File[] fileArray = unzipDir.listFiles();
			if(!ArrayUtils.isEmpty(fileArray)){
				for(File file : fileArray){
					if(!file.delete()){
						log.warn("清除文件失败:" + name);
						return;
					}
				}
			}
			if(!unzipDir.delete()){
				log.warn("清除解压目录失败."); 
				return;
			}
		}

		//srcFile.exist = true
		if(!srcFile.delete()){ 
			log.warn("清除源文件失败."); 
			return;
		}

		if(logFile.exists() && !logFile.delete()){
			log.warn("清除日志失败.");
		}
	}
}
