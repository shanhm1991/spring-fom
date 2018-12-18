package com.fom.context;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipException;

import org.apache.commons.io.FileUtils;

import com.fom.util.ZipUtil;
import com.fom.util.exception.WarnException;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 * @param <E>
 * @param <V>
 */
public abstract class ZipImporter<E extends ZipImporterConfig,V> extends Importer<E,V> {

	protected final File unzipDir;

	private boolean reExecute; 

	private boolean zipValid = true;

	private List<String> nameList = null;

	protected ZipImporter(String name, String path) {
		super(name, path);
		this.unzipDir = new File(path.replace(".zip", ""));
	}

	void execute() throws Exception {
		if(logFile.exists()){
			log.warn("继续处理遗留任务文件."); 
			//上次任务删除源文件失败
			if(!unzipDir.exists()){ 
				reExecute = false;
				return;
			}

			//上次任务删除解压目录失败或者解压失败
			String[] nameArray = unzipDir.list();
			if(nameArray == null || nameArray.length == 0){ 
				reExecute = false;
				return;
			}

			nameList = Arrays.asList(nameArray);
			//上次任务清空解压目录失败
			if(!validZipContent(config, nameList)){
				reExecute = false;
				return;
			}

			//处理未完成文件
			processFailedFile();
		} else {
			//第一次任务处理
			if(!unzipDir.exists()){
				if(!unzipDir.mkdirs()){
					throw new WarnException("创建解压目录失败: " + unzipDir.getName());
				}
			}else{
				//上次任务zip文件未正确解压
				log.warn("清空未正确解压的文件目录: " + unzipDir.getName());
				File[] fileArray = unzipDir.listFiles();
				if(fileArray != null && fileArray.length > 0){
					for(File file : fileArray){
						if(!file.delete()){
							throw new WarnException("清除文件失败:" + file.getName()); 
						}
					}
				}
			}

			long unzipCost = 0;
			try{
				unzipCost = ZipUtil.unZip(srcFile, unzipDir);
			}catch(ZipException e){
				log.error("解压失败", e); 
				zipValid = false;
				return; 
			}
			log.info("解压结束(" + srcSize + "KB), 耗时=" + unzipCost + "ms");

			String[] nameArray = unzipDir.list();
			if(nameArray == null || nameArray.length == 0){ 
				zipValid = false;
				return;
			}

			nameList = Arrays.asList(nameArray);
			if(!validZipContent(config, nameList)){
				zipValid = false;
				return;
			}

			if(!logFile.createNewFile()){
				throw new WarnException("创建日志文件失败.");
			}
		}
		
		processFiles();
	}

	protected boolean validZipContent(E config, List<String> nameList) {
		return true;
	}

	private void processFailedFile() throws Exception {  
		List<String> lines = FileUtils.readLines(logFile);
		//刚创建完日志文件,线程结束
		if(lines.isEmpty()){
			return;
		}

		String name = lines.get(0); 
		File file = new File(unzipDir + File.separator + name);
		log.info("继续处理遗留文件:" + name); 

		if(!file.exists()){
			log.warn("遗留文件已不存在:" + name);
			return;
		}

		long sTime = System.currentTimeMillis();
		long size = file.length() / 1024;
		int lineIndex = 0;
		try{
			lineIndex = Integer.valueOf(lines.get(1));
		}catch(Exception e){
			log.warn("获取文件处理进度失败,将从第0行开始处理:" + name);
		}

		readLine(file, lineIndex);
		nameList.remove(name);
		log.info("处理文件结束[" + file.getName() + "(" + size + "KB)], 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		if(!file.delete()){
			throw new WarnException("删除文件失败:" + name); 
		}
	}

	private void processFiles() throws Exception {
		for(String name : nameList){
			if(!config.matchZipFile(name)){
				continue;
			} 
			long sTime = System.currentTimeMillis();
			File file = new File(unzipDir + File.separator + name);
			long size = file.length() / 1024;
			readLine(file, 0);
			log.info("处理文件结束[" + name + "(" + size + "KB)], 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
			if(!file.delete()){
				throw new WarnException("删除文件失败:" + file.getName()); 
			}
		}
	}

	void finallyExecute() {
		if(zipValid && !logFile.exists()){
			return;
		}

		if(unzipDir.exists()){ 
			String[] nameArray = unzipDir.list();
			if(nameArray != null && nameArray.length > 0){
				if(!reExecute || !validZipContent(config, Arrays.asList(nameArray))){
					for(String name : nameArray){
						if(!new File(unzipDir + File.separator + name).delete()){
							log.warn("清除文件失败:" + name); 
							return;
						}
					}
				}else{
					log.warn("文件遗留, 等待下次处理."); 
					return;
				}
			}

			if(!unzipDir.delete()){
				log.warn("删除解压目录失败."); 
				return;
			}
		}

		if(!srcFile.delete()){ //srcFile.exist = true
			log.warn("删除源文件失败."); 
			return;
		}

		if(logFile.exists() && !logFile.delete()){
			log.warn("删除日志失败."); 
		}
	}
}
