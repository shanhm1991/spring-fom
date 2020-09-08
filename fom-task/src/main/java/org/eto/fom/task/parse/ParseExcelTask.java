package org.eto.fom.task.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eto.fom.util.file.reader.ExcelEventReader;
import org.eto.fom.util.file.reader.ExcelRow;
import org.eto.fom.util.file.reader.ExcelSheetFilter;
import org.eto.fom.util.file.reader.IExcelReader;

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
 * @param <E> 任务执行结果类型
 * 
 * @author shanhm
 *
 */
public abstract class ParseExcelTask<V, E> extends ParseTask<V, E> {

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

	@Override
	protected boolean beforeExec() throws Exception { 
		String logName = new File(id).getName();
		if(StringUtils.isBlank(getName())){
			this.progressLog = new File(System.getProperty("cache.parse") + File.separator + logName + ".log");
		}else{
			this.progressLog = new File(System.getProperty("cache.parse") 
					+ File.separator + getName() + File.separator + logName + ".log");
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
	protected E exec() throws Exception {
		return parseExcel(id, getSourceName(id), rowIndex);
	}

	protected E parseExcel(String sourceUri, String sourceName, int lineIndex) throws Exception {
		long execTime = System.currentTimeMillis();
		try(IExcelReader reader = getExcelReader(sourceUri)){
			reader.setSheetFilter(new ExcelSheetFilter() {
				@Override
				public void resetSheetListForRead(List<String> nameList) {
					ParseExcelTask.this.reRangeSheet(nameList);
				}

				@Override
				public boolean filter(int sheetIndex2, String sheetName) {
					return sheetIndex2 >= ParseExcelTask.this.sheetIndex
							&& ParseExcelTask.this.sheetFilter(sheetIndex2, sheetName); 
				}
			});

			List<V> batchData = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			ExcelRow row = null;
			String sheetName = null;
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

				if(!row.isEmpty()){
					List<V> dataList = parseRowData(row, batchTime);
					if(dataList != null){
						batchData.addAll(dataList);
					}
				}

				if((isBatchBySheet && row.isLastRow()) || (batch > 0 && batchData.size() >= batch)){
					checkInterrupt();
					int size = batchData.size();
					batchProcess(batchData, batchTime); 
					log.info("finish batch[file={}, sheet={}, row={}, size={}], cost={}ms",
							sourceName, sheetName, lineIndex, size, System.currentTimeMillis() - batchTime);
					logProgress(sourceName, sheetIndex, sheetName, lineIndex, false); 
					batchData.clear();
					batchTime = System.currentTimeMillis();
				}
			}
			if(!batchData.isEmpty()){
				checkInterrupt();
				int size = batchData.size();
				batchProcess(batchData, batchTime); 
				log.info("finish batch[file={}, sheet={}, row={}, size={}], cost={}ms",
						sourceName, sheetName, lineIndex, size, System.currentTimeMillis() - batchTime);
				logProgress(sourceName, sheetIndex, sheetName, lineIndex, false); 
			}

			log.info("finish excel({}KB), cost={}ms", formatSize(getSourceSize(id)), System.currentTimeMillis() - execTime);
			logProgress(sourceName, sheetIndex, sheetName, lineIndex, true); 
			return onExcelComplete(sourceUri, sourceName);
		}
	}

	protected IExcelReader getExcelReader(String sourceUri) throws Exception {
		return new ExcelEventReader(getExcelInputStream(sourceUri), IExcelReader.EXCEL_XLSX);
	}

	/**
	 * 单个Excel文件解析完成时的动作
	 * @param sourceUri sourceUri
	 * @param sourceName sourceName
	 * @throws Exception Exception
	 */
	protected abstract E onExcelComplete(String sourceUri, String sourceName) throws Exception;

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
	 * @return 
	 */
	protected void reRangeSheet(List<String> sheetRangeList) {

	}

	/**
	 * 将行字段数据映射成对应的bean或者map
	 * @param row row
	 * @param batchTime 批处理时间
	 * @return 映射结果V列表
	 * @throws Exception Exception
	 */
	protected abstract List<V> parseRowData(ExcelRow row, long batchTime) throws Exception;

	/**
	 * 批处理行数据
	 * @param batchData batchData
	 * @param batchTime batchTime
	 * @throws Exception Exception
	 */
	protected abstract void batchProcess(List<V> batchData, long batchTime) throws Exception;

	@Override
	protected void afterExec(boolean isExecSuccess, E content, Throwable e) throws Exception {
		if(!(deleteSource(id) && deleteProgressLog())){
			log.warn("clean failed.");
		}
	}
}
