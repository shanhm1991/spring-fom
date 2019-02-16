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
import com.fom.context.helper.ParserHelper;
import com.fom.context.reader.Reader;
import com.fom.util.IoUtil;

/**
 * 根据sourceUri解析处理文本文件的执行器
 * 
 * @author shanhm
 *
 */
public class Parser extends Task {

	private int batch;
	
	@SuppressWarnings("rawtypes")
	private ParserHelper helper;

	private File logFile;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 */
	@SuppressWarnings("rawtypes")
	public Parser(String sourceUri, int batch, ParserHelper helper){
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
	public Parser(String sourceUri, int batch, ParserHelper helper, ExceptionHandler exceptionHandler) {
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
	public Parser(String sourceUri, int batch, ParserHelper helper, ResultHandler resultHandler) {
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
	public Parser(String sourceUri, int batch, 
			ParserHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
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
				if(batch > 0 && lineDatas.size() >= batch){
					helper.batchProcessLineData(lineDatas, batchTime); 
					logProcess(id, lineIndex);
					lineDatas.clear();
					batchTime = System.currentTimeMillis();
				}
				helper.praseLineData(lineDatas, line, batchTime);
			}
			if(!lineDatas.isEmpty()){
				helper.batchProcessLineData(lineDatas, batchTime); 
			}
			logProcess(id, lineIndex);
		}finally{
			IoUtil.close(reader);
		}
	}

	private void logProcess(String uri, int lineIndex) throws IOException{ 
		log.info("rows processed: " + lineIndex);
		FileUtils.writeStringToFile(logFile, String.valueOf(lineIndex), false);
	}
}
