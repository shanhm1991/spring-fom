package com.fom.context.task;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.Task;
import com.fom.context.ResultHandler;
import com.fom.context.helper.ParseHelper;
import com.fom.context.reader.Reader;
import com.fom.util.IoUtil;

/**
 * 根据sourceUri解析单个文件的任务实现
 * <br>
 * <br>解析策略：
 * <br>1.检查缓存目录是否存在，没有则创建
 * <br>2.
 * 
 * @author shanhm
 *
 */
public class ParseTask extends Task {

	private int batch;
	
	@SuppressWarnings("rawtypes")
	private ParseHelper helper;

	private File logFile;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 */
	@SuppressWarnings("rawtypes")
	public ParseTask(String sourceUri, int batch, ParseHelper helper){
		super(sourceUri);
		this.batch = batch;
		this.helper = helper;
	}
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	@SuppressWarnings("rawtypes")
	public ParseTask(String sourceUri, int batch, ParseHelper helper, ExceptionHandler exceptionHandler) {
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
	}
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public ParseTask(String sourceUri, int batch, ParseHelper helper, ResultHandler resultHandler) {
		this(sourceUri, batch, helper);
		this.resultHandler = resultHandler;
	}
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public ParseTask(String sourceUri, int batch, 
			ParseHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}
	
	@Override
	protected boolean beforeExec() throws Exception { 
		String logName = new File(id).getName();
		this.logFile = new File(System.getProperty("cache.parse") 
				+ File.separator + contextName + File.separator + logName + ".log");
		File parentFile = logFile.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			log.error("directory create failed: " + parentFile);
			return false;
		}
		return true;
	}

	@Override
	protected boolean exec() throws Exception {
		long sTime = System.currentTimeMillis();
		int lineIndex = 0;
		if(!logFile.exists()){ 
			if(!logFile.createNewFile()){
				log.error("directory create failed.");
				return false;
			}
		}else{
			log.warn("continue to deal with uncompleted task."); 
			List<String> lines = FileUtils.readLines(logFile);
			try{
				lineIndex = Integer.valueOf(lines.get(0));
				log.info("get failed file processed progress: " + lineIndex); 
			}catch(Exception e){
				log.warn("get failed file processed progress failed, will process from first line.");
			}
		}
		read(lineIndex);
		String size = new DecimalFormat("#.###").format(helper.getSourceSize(id));
		log.info("finish file(" + size + "KB), cost=" + (System.currentTimeMillis() - sTime) + "ms");
		return true;
	}
	
	@Override
	protected boolean afterExec() throws Exception {
		if(!helper.delete(id)){ 
			log.error("delete src file failed.");
			return false;
		}
		if(!logFile.delete()){
			log.error("delete logFile failed.");
			return false;
		}
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void read(int StartLine) throws Exception {
		int lineIndex = 0;
		String line = "";
		Reader reader = null;
		try{
			reader = helper.getReader(id);
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
					logProcess(id, lineIndex);
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
			logProcess(id, lineIndex);
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
		log.info("rows processed: " + lineIndex);
		FileUtils.writeStringToFile(logFile, String.valueOf(lineIndex), false);
	}
}
