package com.fom.context.executor;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.Executor;
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
public class Parser extends Executor {

	private String sourceUri;

	private int batch;
	
	@SuppressWarnings("rawtypes")
	private ParserHelper helper;

	private File logFile;

	/**
	 * @param name 模块名称
	 * @param sourceName 资源名称
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 */
	@SuppressWarnings("rawtypes")
	public Parser(String name, String sourceName, String sourceUri, int batch, ParserHelper helper){
		super(name,sourceName);
		this.sourceUri = sourceUri;
		this.batch = batch;
		this.helper = helper;
		this.logFile = new File(System.getProperty("import.progress") 
				+ File.separator + name + File.separator + sourceName + ".log");
	}
	
	/**
	 * @param name 模块名称
	 * @param sourceName 资源名称
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	@SuppressWarnings("rawtypes")
	public Parser(String name, String sourceName, String sourceUri, int batch, 
			ParserHelper helper, ExceptionHandler exceptionHandler) {
		this(name, sourceName, sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
	}
	
	/**
	 * @param name 模块名称
	 * @param sourceName 资源名称
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public Parser(String name, String sourceName, String sourceUri, int batch, 
			ParserHelper helper, ResultHandler resultHandler) {
		this(name, sourceName, sourceUri, batch, helper);
		this.resultHandler = resultHandler;
	}
	
	/**
	 * @param name 模块名称
	 * @param sourceName 资源名称
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public Parser(String name, String sourceName, String sourceUri, int batch, 
			ParserHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(name, sourceName, sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}
	
	@Override
	protected boolean onStart() throws Exception {
		File parentFile = logFile.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			log.error("创建目录失败:" + parentFile);
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
				log.error("创建日志文件失败.");
				return false;
			}
		}else{
			log.warn("继续处理失败任务."); 
			List<String> lines = FileUtils.readLines(logFile);
			try{
				lineIndex = Integer.valueOf(lines.get(0));
				log.info("获取文件处理进度:" + lineIndex); 
			}catch(Exception e){
				log.warn("获取文件处理进度失败,将从第0行开始处理.");
			}
		}
		read(lineIndex);
		String size = new DecimalFormat("#.##").format(helper.getSourceSize(sourceUri));
		log.info("处理文件结束(" + size + "KB),耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		return true;
	}
	
	@Override
	protected boolean onComplete() throws Exception {
		if(!helper.delete(sourceUri)){ 
			log.error("删除源文件失败.");
			return false;
		}
		if(!logFile.delete()){
			log.error("删除日志文件失败.");
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
			reader = helper.getReader(sourceUri);
			List lineDatas = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			while ((line = reader.readLine()) != null) {
				lineIndex++;
				if(lineIndex <= StartLine){
					continue;
				}
				if(batch > 0 && lineDatas.size() >= batch){
					helper.batchProcessLineData(lineDatas, batchTime); 
					logProcess(sourceUri, lineIndex);
					lineDatas.clear();
					batchTime = System.currentTimeMillis();
				}
				helper.praseLineData(lineDatas, line, batchTime);
			}
			if(!lineDatas.isEmpty()){
				helper.batchProcessLineData(lineDatas, batchTime); 
			}
			logProcess(sourceUri, lineIndex);
		}finally{
			IoUtil.close(reader);
		}
	}

	private void logProcess(String uri, int lineIndex) throws IOException{ 
		log.info("已读取行数:" + lineIndex);
		FileUtils.writeStringToFile(logFile, String.valueOf(lineIndex), false);
	}
}
