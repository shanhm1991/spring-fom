package com.fom.context.task;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.Task;
import com.fom.context.ResultHandler;
import com.fom.context.helper.ZipParseHelper;
import com.fom.context.reader.Reader;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 根据sourceUri解析处理本地zip文件的执行器
 * 
 * @author shanhm
 *
 */
public final class ZipParseTask extends Task {
	
	private int batch;
	
	@SuppressWarnings("rawtypes")
	private ZipParseHelper helper;
	
	private File logFile;
	
	private File unzipDir;
	
	private List<String> matchedEntrys;
	
	private DecimalFormat numFormat  = new DecimalFormat("#.###");
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 */
	@SuppressWarnings("rawtypes")
	public ZipParseTask(String sourceUri, int batch, ZipParseHelper helper) {
		super(sourceUri);
		this.helper = helper;
		String sourceName = new File(sourceUri).getName();
		this.unzipDir = new File(System.getProperty("cache.parse")
				+ File.separator + contextName + File.separator + sourceName);
		this.logFile = new File(System.getProperty("cache.parse") 
				+ File.separator + contextName + File.separator + sourceName + ".log");
	}
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	@SuppressWarnings("rawtypes")
	public ZipParseTask(String sourceUri, int batch, 
			ZipParseHelper helper, ExceptionHandler exceptionHandler) { 
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
	}
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public ZipParseTask(String sourceUri, int batch, 
			ZipParseHelper helper, ResultHandler resultHandler) {
		this(sourceUri, batch, helper);
		this.resultHandler = resultHandler;
	}
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public ZipParseTask(String sourceUri, int batch, 
			ZipParseHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}
	
	@Override
	protected boolean beforeExec() throws Exception {
		if(!ZipUtil.valid(id)){ 
			log.error("zip invalid."); 
			if(!new File(id).delete()){
				log.error("zip delete failed."); 
			}
			return false;
		}
		
		File parentFile = logFile.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			log.error("directory create failed:" + parentFile);
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
			log.warn("continue to deal with uncompleted task."); 
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
				log.warn("no file need to be process, clear directly.");
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
					log.warn("clear files which unzip uncorrectly.");
					for(File file : fileArray){
						if(!file.delete()){
							log.error("delete file failed: " + file.getName()); 
							return false;
						}
					}
				}
			}else{
				//首次任务处理
				if(!unzipDir.mkdirs()){
					log.error("directory create failed: " + unzipDir);
					return false;
				}
			}

			long cost = ZipUtil.unZip(id, unzipDir);
			String size = numFormat.format(helper.getSourceSize(id));
			log.info("finish unzip(" + size + "KB), cost=" + cost + "ms");

			String[] nameArray = unzipDir.list();
			if(ArrayUtils.isEmpty(nameArray)){ 
				log.warn("no file need to be process, clear directly.");
				return true;
			}

			matchedEntrys = filterEntrys(nameArray);
			if(matchedEntrys.isEmpty()){
				log.warn("no file need to be process, clear directly.");
				return true;
			}

			if(!logFile.createNewFile()){
				log.warn("logFile create failed.");
				return false;
			}
		}
		return processFiles();
	}
	
	@Override
	protected boolean afterExec() throws Exception {
		if(unzipDir.exists()){ 
			File[] fileArray = unzipDir.listFiles();
			if(!ArrayUtils.isEmpty(fileArray)){
				for(File file : fileArray){
					if(!file.delete()){
						log.warn("clear temp file failed: " + file.getName()); 
						return false;
					}
				}
			}
			if(!unzipDir.delete()){
				log.warn("clear temp directory failed."); 
				return false;
			}
		}
		//srcFile.exist = true
		if(!helper.delete(id)){ 
			log.warn("clear src file failed."); 
			return false;
		}
		if(logFile.exists() && !logFile.delete()){
			log.warn("clear logFile failed.");
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
			log.warn("cann't find failed file: " + name);
			return true;
		}

		long sTime = System.currentTimeMillis();
		int lineIndex = 0;
		try{
			lineIndex = Integer.valueOf(lines.get(1));
			log.info("get failed file processed progress[" + name + "]: " + lineIndex); 
		}catch(Exception e){
			log.warn("get failed file processed progress failed, will process from first line: " + name);
		}

		read(file.getPath(), lineIndex); 
		matchedEntrys.remove(name);
		
		String size = numFormat.format(file.length() / 1024.0);
		log.info("finish file[" + name + "(" + size + "KB)], cost=" + (System.currentTimeMillis() - sTime) + "ms");
		if(!file.delete()){
			log.error("delete file failed: " + name); 
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
			log.info("finish file[" + name + "(" + size + "KB)], cost=" + (System.currentTimeMillis() - sTime) + "ms");
			if(!file.delete()){
				log.error("delete file failed: " + file.getName()); 
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
				if(batch > 0 && lineDatas.size() >= batch && notInterruped()){
					int size = lineDatas.size();
					helper.batchProcessLineData(lineDatas, batchTime); 
					log.info("批处理结束[" + size + "],耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
					logProcess(uri, lineIndex);
					lineDatas.clear();
					batchTime = System.currentTimeMillis();
				}
				helper.praseLineData(lineDatas, line, batchTime);
			}
			if(!lineDatas.isEmpty() && notInterruped()){
				int size = lineDatas.size();
				helper.batchProcessLineData(lineDatas, batchTime); 
				log.info("批处理结束[" + size + "],耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
			}
			logProcess(uri, lineIndex);
		}finally{
			IoUtil.close(reader);
		}
	}
	
	private boolean notInterruped() throws InterruptedException{
		if(Thread.interrupted()){
			throw new InterruptedException("interrupted when batchProcessLineData");
		}
		return true;
	}

	private void logProcess(String uri, int lineIndex) throws IOException{ 
		String data = uri + "\n" + lineIndex;
		log.info("rows processed: " + lineIndex);
		FileUtils.writeStringToFile(logFile, data, false);
	}
		
}
