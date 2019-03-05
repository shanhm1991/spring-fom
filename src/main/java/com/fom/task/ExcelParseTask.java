package com.fom.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.ResultHandler;
import com.fom.context.Task;
import com.fom.task.helper.ExcelParseHelper;
import com.fom.task.reader.ExcelReader;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.util.IoUtil;

/**
 * 根据sourceUri解析单个Excel文件的任务实现
 * <br>
 * <br>解析策略：
 * <br>1.检查缓存目录是否存在，没有则创建
 * <br>2.检查缓存目录下是否存在progressLog（纪录任务处理进度），没有则从第0sheet第0行开始读取，有则读取progressLog中的处理进度n,
 * <br>3.逐行读取解析成指定的bean或者map，放入lineDatas中
 * <br>4.当lineDatas的size达到batch时（batch为0时则读取所有），进行批量处理，处理结束后纪录进度到progressLog，然后重复步骤3
 * <br>5.删除源文件，删除progressLog
 * <br>上述任何步骤失败或异常均会使任务提前失败结束
 * 
 * @see ExcelParseHelper
 * 
 * @param <V> 行数据解析结果类型
 * 
 * @author shanhm
 *
 */
public class ExcelParseTask<V> extends Task {


	private int batch;

	private ExcelParseHelper<V> helper;

	protected int sheetIndex = 0;

	protected int rowIndex = 0;

	protected File progressLog;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ExcelParseHelper
	 */
	public ExcelParseTask(String sourceUri, int batch, ExcelParseHelper<V> helper){
		super(sourceUri);
		this.batch = batch;
		this.helper = helper;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ExcelParseHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	public ExcelParseTask(String sourceUri, int batch, ExcelParseHelper<V> helper, ExceptionHandler exceptionHandler) {
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ExcelParseHelper
	 * @param resultHandler ResultHandler
	 */
	public ExcelParseTask(String sourceUri, int batch, ExcelParseHelper<V> helper, ResultHandler resultHandler) {
		this(sourceUri, batch, helper);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ExcelParseHelper
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public ExcelParseTask(String sourceUri, int batch, 
			ExcelParseHelper<V> helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
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
				sheetIndex = Integer.valueOf(lines.get(0));
				rowIndex = Integer.valueOf(lines.get(1));
				log.info("get failed file processed progress: sheet=" + sheetIndex + ",rowIndex=" +rowIndex); 
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
		String size = new DecimalFormat("#.###").format(helper.getSourceSize(id));
		log.info("finish excel(" + size + "KB), cost=" + (System.currentTimeMillis() - sTime) + "ms");
		return true;
	}

	@Override
	protected boolean afterExec() throws Exception {
		if(!helper.delete(id)){ 
			log.error("delete src file failed.");
			return false;
		}
		if(!progressLog.delete()){
			log.error("delete progress log failed.");
			return false;
		}
		return true;
	}

	private void parse() throws Exception {
		Reader reader = null;
		RowData rowData = null;
		String sheetName = null;
		try{
			reader = new InnerReader(helper.getInputStream(id), helper.getExcelType());
			List<V> batchData = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			while ((rowData = reader.readRow()) != null) {
				if(sheetIndex < rowData.getSheetIndex()){
					sheetIndex = rowData.getSheetIndex();
					rowIndex = 0;
				}

				if(rowIndex > 0 && rowData.getRowIndex() <= rowIndex){
					continue;
				}
				rowIndex = rowData.getRowIndex();
				sheetName = rowData.getSheetName();

				if (log.isDebugEnabled()) {
					log.debug("parse row[sheetName=" + sheetName + ",sheetIndex=" + sheetIndex 
							+ ",rowIndex=" + rowIndex + "],columns=" + rowData.getColumnList());
				}
				List<V> dataList = helper.parseRowData(rowData, batchTime);
				if(dataList != null){
					batchData.addAll(dataList);
				}

				if(rowData.isLastRow() || (batch > 0 && batchData.size() >= batch)){
					TaskUtil.checkInterrupt();
					int size = batchData.size();
					helper.batchProcess(batchData, batchTime); 
					TaskUtil.log(log, progressLog, String.valueOf(sheetIndex), size); 
					batchData.clear();
					batchTime = System.currentTimeMillis();
					log.info("finish batch[sheetName=" + sheetName + ",sheetIndex=" + sheetIndex 
							+ ",size=" + size + "],cost=" + (System.currentTimeMillis() - batchTime) + "ms");
				}
			}
			if(!batchData.isEmpty()){
				TaskUtil.checkInterrupt();
				int size = batchData.size();
				helper.batchProcess(batchData, batchTime); 
				TaskUtil.log(log, progressLog, String.valueOf(sheetIndex), size); 
				log.info("finish batch[sheetName=" + sheetName + ",sheetIndex=" + sheetIndex 
						+ ",size=" + size + "],cost=" + (System.currentTimeMillis() - batchTime) + "ms");
			}
		}finally{
			IoUtil.close(reader);
		}
	}

	private class InnerReader extends ExcelReader { 

		public InnerReader(InputStream inputStream, String type) throws IOException {
			super(inputStream, type);
		}

		@Override
		protected boolean shouldSheetProcess(int sheetIndex, String sheetName) {
			return sheetIndex >= ExcelParseTask.this.sheetIndex
					&& helper.sheetFilter(sheetIndex, sheetName); 
		}
	}
}
