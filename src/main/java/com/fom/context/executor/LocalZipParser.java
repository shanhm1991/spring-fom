package com.fom.context.executor;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.Executor;
import com.fom.context.ResultHandler;
import com.fom.context.helper.LocalZipParserHelper;
import com.fom.context.reader.Reader;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 根据sourceUri解析处理本地zip文件的执行器
 * 
 * @author shanhm
 *
 */
public final class LocalZipParser extends Executor {
	
	private String sourceUri;
	
	private int batch;
	
	@SuppressWarnings("rawtypes")
	private LocalZipParserHelper helper;
	
	private File logFile;
	
	private File unzipDir;
	
	private List<String> matchedEntrys;
	
	private DecimalFormat numFormat  = new DecimalFormat("#.##");
	
	/**
	 * @param name 模块名称
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 */
	@SuppressWarnings("rawtypes")
	public LocalZipParser(String name, String sourceUri, int batch, LocalZipParserHelper helper) {
		super(name, sourceUri);
		this.sourceUri = sourceUri;
		this.helper = helper;
		String sourceName = new File(sourceUri).getName();
		this.unzipDir = new File(System.getProperty("import.progress")
				+ File.separator + name + File.separator + sourceName);
		this.logFile = new File(System.getProperty("import.progress") 
				+ File.separator + name + File.separator + sourceName + ".log");
	}
	
	/**
	 * @param name 模块名称
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	@SuppressWarnings("rawtypes")
	public LocalZipParser(String name, String sourceUri, int batch, 
			LocalZipParserHelper helper, ExceptionHandler exceptionHandler) { 
		this(name, sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
	}
	
	/**
	 * @param name 模块名称
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public LocalZipParser(String name, String sourceUri, int batch, 
			LocalZipParserHelper helper, ResultHandler resultHandler) {
		this(name, sourceUri, batch, helper);
		this.resultHandler = resultHandler;
	}
	
	/**
	 * @param name 模块名称
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public LocalZipParser(String name, String sourceUri, int batch, 
			LocalZipParserHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(name, sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}
	
	@Override
	protected boolean onStart() throws Exception {
		if(!ZipUtil.valid(sourceUri)){ 
			log.error("zip校验失败."); 
			if(!new File(sourceUri).delete()){
				log.error("zip清除失败."); 
			}
			return false;
		}
		
		File parentFile = logFile.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			log.error("创建目录失败:" + parentFile);
			return false;
		}
		return true;
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
	protected boolean exec() throws Exception {
		if(logFile.exists()){
			log.warn("继续处理失败任务."); 
			//失败在第5步
			if(!unzipDir.exists()){ 
				return true;
			}
			String[] nameArray = unzipDir.list();
			//失败在第4步
			if(ArrayUtils.isEmpty(nameArray)){  
				return true;
			}
			matchedEntrys = filterEntrys(nameArray);
			//失败在第4步
			if(matchedEntrys.isEmpty()){
				log.warn("没有需要处理的文件,直接清除.");
				return true;
			}
			//失败在第3步,继续处理未完成的文件
			if(!processFailedFile()){
				return false;
			}
		} else {
			if(unzipDir.exists()){
				File[] fileArray = unzipDir.listFiles();
				if(!ArrayUtils.isEmpty(fileArray)){
					//失败在第1、2步，清除并重新尝试
					log.warn("清空未正确解压的文件目录");
					for(File file : fileArray){
						if(!file.delete()){
							log.error("清除文件失败:" + file.getName()); 
							return false;
						}
					}
				}
			}else{
				//首次任务处理
				if(!unzipDir.mkdirs()){
					log.error("创建目录失败:" + unzipDir);
					return false;
				}
			}

			long cost = ZipUtil.unZip(sourceUri, unzipDir);
			String size = numFormat.format(helper.getSourceSize(sourceUri));
			log.info("解压结束(" + size + "KB), 耗时=" + cost + "ms");

			String[] nameArray = unzipDir.list();
			if(ArrayUtils.isEmpty(nameArray)){ 
				log.warn("没有需要处理的文件,直接清除.");
				return true;
			}

			matchedEntrys = filterEntrys(nameArray);
			if(matchedEntrys.isEmpty()){
				log.warn("没有需要处理的文件,直接清除.");
				return true;
			}

			if(!logFile.createNewFile()){
				log.warn("创建日志文件失败.");
				return false;
			}
		}
		return processFiles();
	}
	
	@Override
	protected boolean onComplete() throws Exception {
		if(unzipDir.exists()){ 
			File[] fileArray = unzipDir.listFiles();
			if(!ArrayUtils.isEmpty(fileArray)){
				for(File file : fileArray){
					if(!file.delete()){
						log.warn("清除临时文件失败."); 
						return false;
					}
				}
			}
			if(!unzipDir.delete()){
				log.warn("清除临时目录失败."); 
				return false;
			}
		}
		//srcFile.exist = true
		if(!helper.delete(sourceUri)){ 
			log.warn("清除源文件失败."); 
			return false;
		}
		if(logFile.exists() && !logFile.delete()){
			log.warn("清除日志失败.");
		}
		return true;
	}

	private List<String> filterEntrys(String[] nameArray){
		List<String> list = new LinkedList<>();
		for(String name : nameArray){
			if(helper.matchEntryName(name)){
				list.add(name);
			}
		}
		return list;
	}

	private boolean processFailedFile() throws Exception {  
		List<String> lines = FileUtils.readLines(logFile);
		if(lines.isEmpty()){
			return true;
		}

		String name = lines.get(0); 
		File file = new File(unzipDir + File.separator + name);
		if(!file.exists()){
			log.warn("未找到记录文件:" + name);
			return true;
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
			log.error("删除临时文件失败:" + name); 
			return false;
		}
		return true;
	}

	private boolean processFiles() throws Exception {
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
				log.error("删除临时文件失败:" + file.getName()); 
				return false;
			}
		}
		return true;
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
				if(batch > 0 && lineDatas.size() >= batch){
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
		
}
