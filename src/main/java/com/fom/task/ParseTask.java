package com.fom.task;

import java.io.File;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.ResultHandler;
import com.fom.context.Task;
import com.fom.task.helper.TextParseHelper;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.util.IoUtil;

/**
 * 根据sourceUri解析单个文件的任务实现
 * <br>
 * <br>解析策略：
 * <br>1.检查缓存目录是否存在，没有则创建
 * <br>2.检查缓存目录下是否存在logFile（纪录任务处理进度），没有则从第0行开始读取，有则读取logFile中的处理进度n，从第n行开始
 * <br>3.逐行读取解析成指定的bean或者map，放入lineDatas中
 * <br>4.当lineDatas的size达到batch时（batch为0时则读取所有），进行批量处理，处理结束后纪录进度到logFile，然后重复步骤3
 * <br>5.删除源文件，删除logFile
 * <br>上述任何步骤失败或异常均会使任务提前失败结束
 * 
 * @see TextParseHelper
 * 
 * @param <V> 行数据解析结果类型
 * 
 * @author shanhm
 *
 */
public class ParseTask<V> extends Task {

	private int batch;

	private TextParseHelper<V> helper;

	private File progressLog;

	private int rowIndex = 0;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 */
	public ParseTask(String sourceUri, int batch, TextParseHelper<V> helper){
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
	public ParseTask(String sourceUri, int batch, TextParseHelper<V> helper, ExceptionHandler exceptionHandler) {
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 * @param resultHandler ResultHandler
	 */
	public ParseTask(String sourceUri, int batch, TextParseHelper<V> helper, ResultHandler resultHandler) {
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
	public ParseTask(String sourceUri, int batch, 
			TextParseHelper<V> helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	protected boolean beforeExec() throws Exception { 
		String logName = new File(id).getName();
		if(StringUtils.isBlank(getContextName())){
			this.progressLog = new File(System.getProperty("cache.parse") + File.separator + logName + ".log");
		}else{
			this.progressLog = new File(System.getProperty("cache.parse") 
					+ File.separator + getContextName() + File.separator + logName + ".log");
		}

		File parentFile = progressLog.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			log.error("directory create failed: " + parentFile);
			return false;
		}
		
		if(!progressLog.exists()){ 
			if(!progressLog.createNewFile()){
				log.error("progress log create failed.");
				return false;
			}
		}else{
			log.warn("continue to deal with uncompleted task."); 
			List<String> lines = FileUtils.readLines(progressLog);
			try{
				rowIndex = Integer.valueOf(lines.get(0));
				log.info("get history processed progress: rowIndex=" + rowIndex); 
			}catch(Exception e){
				log.warn("get history processed progress failed, will process from scratch.");
			}
		}
		
		return true;
	}

	@Override
	protected boolean exec() throws Exception {
		long sTime = System.currentTimeMillis();
		parse();
		if (log.isDebugEnabled()) {
			String size = new DecimalFormat("#.###").format(helper.getSourceSize(id));
			log.debug("finish parse(" + size + "KB), cost=" + (System.currentTimeMillis() - sTime) + "ms");
		}
		return true;
	}

	@Override
	protected boolean afterExec() throws Exception {
		if(!helper.delete(id)){ 
			log.error("delete src file failed.");
			return false;
		}
		if(!progressLog.delete()){
			log.error("delete logFile failed.");
			return false;
		}
		return true;
	}

	private void parse() throws Exception {
		Reader reader = null;
		RowData rowData = null;
		try{
			reader = helper.getReader(id);
			List<V> batchData = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			while ((rowData = reader.readRow()) != null) {
				if(rowIndex > 0 && rowData.getRowIndex() <= rowIndex){
					continue;
				}
				rowIndex = rowData.getRowIndex();

				List<V> dataList = helper.parseRowData(rowData, batchTime);
				if(dataList != null){
					batchData.addAll(dataList);
				}

				if(batch > 0 && batchData.size() >= batch){
					TaskUtil.checkInterrupt();
					int size = batchData.size();
					helper.batchProcess(batchData, batchTime); 
					TaskUtil.log(log, progressLog, rowIndex);
					batchData.clear();
					batchTime = System.currentTimeMillis();
					if (log.isDebugEnabled()) {
						log.debug("批处理结束[" + size + "],耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
					}
				}
			}
			if(!batchData.isEmpty()){
				TaskUtil.checkInterrupt();
				int size = batchData.size();
				helper.batchProcess(batchData, batchTime);  
				TaskUtil.log(log, progressLog, rowIndex);
				if (log.isDebugEnabled()) {
					log.debug("批处理结束[" + size + "],耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
				}
			}
		}finally{
			IoUtil.close(reader);
		}
	}

}
