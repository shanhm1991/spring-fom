package com.fom.context.executor;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.fom.context.Executor;
import com.fom.context.exception.WarnException;
import com.fom.context.executor.helper.ImporterHelper;
import com.fom.context.executor.reader.Reader;
import com.fom.log.LoggerFactory;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalZipImporter implements Executor {
	
	private Logger log;
	
	private String sourceUri;
	
	private LocalZipImporterConfig config;
	
	@SuppressWarnings("rawtypes")
	private ImporterHelper helper;
	
	private File logFile;
	
	private File unzipDir;
	
	private boolean removeDirectly;

	private List<String> matchedEntrys;
	
	private DecimalFormat numFormat  = new DecimalFormat("#.##");
	
	@SuppressWarnings("rawtypes")
	public LocalZipImporter(String name, String sourceUri, LocalZipImporterConfig config, ImporterHelper helper) {
		this.log = LoggerFactory.getLogger(name);
		this.sourceUri = sourceUri;
		this.helper = helper;
		this.config = config;
		String sourceName = new File(sourceUri).getName();
		this.unzipDir = new File(System.getProperty("import.progress")
				+ File.separator + name + File.separator + sourceName);
		this.logFile = new File(System.getProperty("import.progress") 
				+ File.separator + name + File.separator + sourceName + ".log");
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
	@Override
	public void exec() throws Exception {
		try{
			process();
		}finally{
			onFinally();
		}
	}
	
	private void process() throws Exception {
		if(!ZipUtil.valid(sourceUri)){ 
			log.error("zip校验失败，直接清除."); 
			removeDirectly = true;
			return;
		}
		
		File parentFile = logFile.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			throw new WarnException("创建目录失败:" + parentFile);
		}
		
		if(logFile.exists()){
			log.warn("继续处理失败任务."); 
			if(!unzipDir.exists()){ 
				removeDirectly = true;
				//失败在第5步
				return;
			}
			String[] nameArray = unzipDir.list();
			if(ArrayUtils.isEmpty(nameArray)){  
				removeDirectly = true;
				//失败在第4步
				return;
			}
			matchedEntrys = filterEntrys(nameArray);
			if(matchedEntrys.isEmpty()){
				log.warn("没有需要处理的文件,直接清除.");
				removeDirectly = true;
				//失败在第4步
				return;
			}
			//失败在第3步,继续处理未完成的文件
			processFailedFile();
		} else {
			if(unzipDir.exists()){
				File[] fileArray = unzipDir.listFiles();
				if(!ArrayUtils.isEmpty(fileArray)){
					//失败在第1、2步，清除并重新尝试
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
					throw new WarnException("创建目录失败: " + unzipDir.getName());
				}
			}

			long cost = ZipUtil.unZip(sourceUri, unzipDir);
			String size = numFormat.format(helper.getSourceSize(sourceUri));
			log.info("解压结束(" + size + "KB), 耗时=" + cost + "ms");

			String[] nameArray = unzipDir.list();
			if(ArrayUtils.isEmpty(nameArray)){ 
				removeDirectly = true;
				return;
			}

			matchedEntrys = filterEntrys(nameArray);
			if(matchedEntrys.isEmpty()){
				log.warn("没有需要处理的文件,直接清除.");
				removeDirectly = true;
				return;
			}

			if(!logFile.createNewFile()){
				throw new WarnException("创建日志文件失败.");
			}
		}

		processFiles();
	}

	private List<String> filterEntrys(String[] nameArray){
		List<String> list = new LinkedList<>();
		for(String name : nameArray){
			if(config.matchEntryName(name)){
				list.add(name);
			}
		}
		return list;
	}

	private void processFailedFile() throws Exception {  
		List<String> lines = FileUtils.readLines(logFile);
		if(lines.isEmpty()){
			return;
		}

		String name = lines.get(0); 
		File file = new File(unzipDir + File.separator + name);
		if(!file.exists()){
			log.warn("未找到记录文件:" + name);
			return;
		}

		long sTime = System.currentTimeMillis();
		int lineIndex = 0;
		try{
			lineIndex = Integer.valueOf(lines.get(1));
			log.info("获取文件处理进度[" + name + "]:" + lineIndex); 
		}catch(Exception e){
			log.warn("获取文件处理进度失败,将从第0行开始处理:" + name);
		}

		read(file.getPath(), lineIndex); 
		matchedEntrys.remove(name);
		
		String size = numFormat.format(file.length() / 1024.0);
		log.info("处理文件结束[" + name + "(" + size + "KB)], 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		if(!file.delete()){
			throw new WarnException("删除文件失败:" + name); 
		}
	}

	private void processFiles() throws Exception {
		Iterator<String> it = matchedEntrys.iterator();
		while(it.hasNext()){
			String name = it.next();
			long sTime = System.currentTimeMillis();
			File file = new File(unzipDir + File.separator + name);
			
			read(file.getPath(), 0);
			it.remove();
			
			String size = numFormat.format(file.length() / 1024.0);
			log.info("处理文件结束[" + name + "(" + size + "KB)], 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
			if(!file.delete()){
				throw new WarnException("删除文件失败:" + file.getName()); 
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void read(String uri, int StartLine) throws Exception {
		int lineIndex = 0;
		String line = "";
		Reader reader = null;
		try{
			reader = helper.getReader(uri);
			List lineDatas = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			while ((line = reader.readLine()) != null) {
				lineIndex++;
				if(lineIndex <= StartLine){
					continue;
				}
				if(config.getBatch() > 0 && lineDatas.size() >= config.getBatch()){
					helper.batchProcessLineData(lineDatas, batchTime); 
					logProcess(uri, lineIndex);
					lineDatas.clear();
					batchTime = System.currentTimeMillis();
				}
				helper.praseLineData(lineDatas, line, batchTime);
			}
			if(!lineDatas.isEmpty()){
				helper.batchProcessLineData(lineDatas, batchTime); 
			}
			logProcess(uri, lineIndex);
		}finally{
			IoUtil.close(reader);
		}
	}

	private void logProcess(String uri, int lineIndex) throws IOException{ 
		String data = uri + "\n" + lineIndex;
		log.info("已读取行数:" + lineIndex);
		FileUtils.writeStringToFile(logFile, data, false);
	}

	private void onFinally() {
		if(!removeDirectly && !matchedEntrys.isEmpty()){
			log.warn("遗留任务文件, 等待下次处理."); 
			return;
		}

		if(unzipDir.exists()){ 
			File[] fileArray = unzipDir.listFiles();
			if(!ArrayUtils.isEmpty(fileArray)){
				for(File file : fileArray){
					if(!file.delete()){
						log.warn("清除临时文件失败."); 
						return;
					}
				}
			}
			if(!unzipDir.delete()){
				log.warn("清除临时文件失败."); 
				return;
			}
		}

		//srcFile.exist = true
		if(!helper.delete(sourceUri)){ 
			log.warn("清除源文件失败."); 
			return;
		}

		if(logFile.exists() && !logFile.delete()){
			log.warn("清除日志失败.");
		}
	}
}
