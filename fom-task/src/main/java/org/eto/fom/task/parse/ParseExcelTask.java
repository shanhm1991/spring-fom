package org.eto.fom.task.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eto.fom.context.ExceptionHandler;
import org.eto.fom.context.ResultHandler;
import org.eto.fom.util.IoUtil;
import org.eto.fom.util.file.reader.ExcelReader;
import org.eto.fom.util.file.reader.Reader;
import org.eto.fom.util.file.reader.ReaderRow;

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
 * @param <V> 行数据解析结果类型
 * 
 * @author shanhm
 *
 */
public abstract class ParseExcelTask<V> extends ParseTask<V> {

	private boolean isBatchBySheet;

	protected int sheetIndex = 0;

	protected int rowIndex = 0;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param isBatchBySheet isBatchBySheet
	 */
	public ParseExcelTask(String sourceUri, int batch, boolean isBatchBySheet){
		super(sourceUri, batch);
		this.isBatchBySheet = isBatchBySheet;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param isBatchBySheet isBatchBySheet
	 * @param exceptionHandler ExceptionHandler
	 */
	public ParseExcelTask(String sourceUri, int batch, boolean isBatchBySheet, ExceptionHandler exceptionHandler) {
		this(sourceUri, batch, isBatchBySheet);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param isBatchBySheet isBatchBySheet
	 * @param resultHandler ResultHandler
	 */
	public ParseExcelTask(String sourceUri, int batch, boolean isBatchBySheet, ResultHandler<Boolean> resultHandler) {
		this(sourceUri, batch, isBatchBySheet);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param isBatchBySheet isBatchBySheet
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public ParseExcelTask(String sourceUri, int batch, boolean isBatchBySheet, 
			ExceptionHandler exceptionHandler, ResultHandler<Boolean> resultHandler) {
		this(sourceUri, batch, isBatchBySheet);
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
			throw new RuntimeException("cache directory create failed: " + parentFile);
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
				log.info("get failed file processed progress: sheetIndex={},rowIndex={}", sheetIndex, rowIndex); 
			}catch(Exception e){
				log.warn("get history processed progress failed, will process from scratch.");
			}
		}

		return true;
	}

	@Override
	protected Boolean exec() throws Exception {
		long sTime = System.currentTimeMillis();
		parseExcel(id, getSourceName(id), rowIndex);
		log.info("finish excel({}KB), cost={}ms", formatSize(getSourceSize(id)), System.currentTimeMillis() - sTime);
		return true;
	}

	protected void parseExcel(String sourceUri, String sourceName, int lineIndex) throws Exception {
		Reader reader = null;
		ReaderRow row = null;
		String sheetName = null;
		try{
			reader = getExcelReader(sourceUri);
			List<V> batchData = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			while ((row = reader.readRow()) != null) {
				if(sheetIndex < row.getSheetIndex()){
					sheetIndex = row.getSheetIndex();
					lineIndex = 0;
				}

				if(lineIndex > 0 && row.getRowIndex() <= lineIndex){
					continue;
				}
				lineIndex = row.getRowIndex();
				sheetName = row.getSheetName();

				if (log.isDebugEnabled()) {
					log.debug("parse row[file={}, sheet={}, row={}], columns={}",
							sourceName, sheetName, rowIndex, row.getColumnList());
				}

				if(row.isEmpty()){
					log.debug("ignore empty row, sheet={}, row={}", sheetName, lineIndex);
				}else{
					List<V> dataList = parseRowData(row, batchTime);
					if(dataList != null){
						batchData.addAll(dataList);
					}
				}

				if((isBatchBySheet && row.isLastRow()) || (batch > 0 && batchData.size() >= batch)){
					checkInterrupt();
					int size = batchData.size();
					batchProcess(batchData, batchTime); 
					log.info("finish batch[file={}, sheet={}, size={}], cost={}ms",
							sourceName, sheetName, size, System.currentTimeMillis() - batchTime);
					logProgress(sourceName, sheetIndex, sheetName, lineIndex, false); 
					batchData.clear();
					batchTime = System.currentTimeMillis();
				}
			}
			if(!batchData.isEmpty()){
				checkInterrupt();
				int size = batchData.size();
				batchProcess(batchData, batchTime); 
				log.info("finish batch[file={}, sheet={}, size={}], cost={}ms",
						sourceName, sheetName, size, System.currentTimeMillis() - batchTime);
				logProgress(sourceName, sheetIndex, sheetName, lineIndex, false); 
			}

			onExcelComplete(sourceUri, sourceName);
			logProgress(sourceName, sheetIndex, sheetName, lineIndex, true); 
		}finally{
			IoUtil.close(reader);
		}
	}

	/**
	 * 单个Excel文件解析完成时的动作
	 * @param sourceUri sourceUri
	 * @param sourceName sourceName
	 * @throws Exception Exception
	 */
	protected void onExcelComplete(String sourceUri, String sourceName) throws Exception {

	}

	/**
	 * 纪录处理进度
	 * @param file file
	 * @param sheetIndex sheetIndex
	 * @param sheetName sheetName
	 * @param row row
	 * @param completed completed
	 * @throws IOException IOException
	 */
	protected void logProgress(String file, int sheetIndex, String sheetName, long row, boolean completed) throws IOException {
		log.info("process progress: file={},sheet={},row={},completed={}", file, sheetName, row, completed);
		if(progressLog != null && progressLog.exists()){
			FileUtils.writeStringToFile(progressLog, file + "\n" + sheetIndex + "\n" + row + "\n" + completed, false);
		}
	}

	/**
	 * 获取对应文件的InputStream
	 * @param sourceUri 资源uri
	 * @return InputStream
	 * @throws Exception Exception
	 */
	protected abstract InputStream getExcelInputStream(String sourceUri) throws Exception;

	/**
	 * Excel类型  xls or xlsx
	 * @return  xls or xlsx
	 */
	protected abstract String getExcelType();

	/**
	 * 过滤需要处理的sheet页
	 * @param sheetIndex sheetIndex
	 * @param sheetName sheetName
	 * @return boolean
	 */
	protected boolean sheetFilter(int sheetIndex, String sheetName) {
		return true;
	}

	/**
	 * 自定义sheet处理顺序
	 * @param sheetRangeList 原sheet顺序
	 * @return 重排序后sheet顺序
	 */
	protected List<String> reRangeSheet(List<String> sheetRangeList) {
		return sheetRangeList;
	}

	private ExcelReader getExcelReader(String sourceUri) throws Exception {

		return new ExcelReader(getExcelInputStream(sourceUri), getExcelType()) {

			@Override
			protected boolean shouldSheetProcess(int sheetIndex, String sheetName) {
				return sheetIndex >= ParseExcelTask.this.sheetIndex
						&& ParseExcelTask.this.sheetFilter(sheetIndex, sheetName); 
			}

			@Override
			protected List<String> reRangeSheet(List<String> sheetRangeList) {
				return ParseExcelTask.this.reRangeSheet(sheetRangeList);
			}
		};
	}

	@Override
	protected boolean afterExec(Boolean execResult) throws Exception {
		return deleteSource(id) && deleteProgressLog();
	}
}