package org.eto.fom.task.parse;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eto.fom.context.core.ExceptionHandler;
import org.eto.fom.context.core.ResultHandler;
import org.eto.fom.util.IoUtil;
import org.eto.fom.util.file.reader.IReader;
import org.eto.fom.util.file.reader.IRow;

/**
 * 根据sourceUri解析单个文件的任务实现
 * <br>
 * <br>解析策略：
 * <br>1.检查缓存目录是否存在，没有则创建
 * <br>2.检查缓存目录下是否存在progressLog（纪录任务处理进度），没有则从第0行开始读取，有则读取progressLog中的处理进度n，从第n行开始
 * <br>3.逐行读取解析成指定的bean或者map，放入lineDatas中
 * <br>4.当lineDatas的size达到batch时（batch为0时则读取所有），进行批量处理，处理结束后纪录进度到progressLog，然后重复步骤3
 * <br>5.删除源文件，删除progressLog
 * <br>上述任何步骤失败或异常均会使任务提前失败结束
 * 
 * @param <V> 解析任务行数据解析结果类型
 * @param <E> 任务执行结果类型
 * 
 * @author shanhm
 *
 */
public abstract class ParseTextTask<V, E> extends ParseTask<V, E> {

	private int rowIndex = 0;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 */
	public ParseTextTask(String sourceUri, int batch){
		super(sourceUri, batch);
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param exceptionHandler ExceptionHandler
	 */
	public ParseTextTask(String sourceUri, int batch, ExceptionHandler exceptionHandler) {
		this(sourceUri, batch);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param resultHandler ResultHandler
	 */
	public ParseTextTask(String sourceUri, int batch, ResultHandler<E> resultHandler) {
		this(sourceUri, batch);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public ParseTextTask(String sourceUri, int batch, 
			ExceptionHandler exceptionHandler, ResultHandler<E> resultHandler) {
		this(sourceUri, batch);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
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
				rowIndex = Integer.valueOf(lines.get(1));
				log.info("get history processed progress: rowIndex={}", rowIndex); 
			}catch(Exception e){
				log.warn("get history processed progress failed, will process from scratch.");
			}
		}
		return true;
	}

	@Override
	protected E exec() throws Exception {
		long sTime = System.currentTimeMillis();
		E e = parseTxt(id, getSourceName(id), rowIndex);
		log.info("finish file({}KB), cost={}ms", formatSize(getSourceSize(id)), System.currentTimeMillis() - sTime);
		return e;
	}

	/**
	 * 纪录处理进度
	 * @param file file
	 * @param row row
	 * @param completed completed
	 * @throws IOException IOException
	 */
	protected void logProgress(String file, long row, boolean completed) throws IOException {
		log.info("process progress: file={},row={},completed={}", file, row, completed);
		if(progressLog != null && progressLog.exists()){
			FileUtils.writeStringToFile(progressLog, file + "\n" + row + "\n" + completed, false);
		}
	}

	/**
	 * 获取对应sourceUri的资源的Reader
	 * @param sourceUri sourceUri
	 * @return Reader
	 * @throws Exception Exception
	 */
	protected abstract IReader getReader(String sourceUri) throws Exception;

	protected E parseTxt(String sourceUri, String sourceName, int lineIndex) throws Exception {
		IReader reader = null;
		IRow row = null;
		long batchTime = System.currentTimeMillis();
		try{
			reader = getReader(sourceUri); 
			List<V> batchData = new LinkedList<>(); 
			while ((row = reader.readRow()) != null) {
				if(lineIndex > 0 && row.getRowIndex() <= lineIndex){
					continue;
				}
				lineIndex = row.getRowIndex();
				if (log.isDebugEnabled()) {
					log.debug("parse row[file={}, row={}], columns={}", sourceName, rowIndex, row.getColumnList());
				}

				List<V> dataList = parseRowData(row, batchTime);
				if(dataList != null){
					batchData.addAll(dataList);
				}

				if(batch > 0 && batchData.size() >= batch){
					checkInterrupt();
					int size = batchData.size();
					batchProcess(batchData, batchTime); 
					log.info("finish batch[file={}, size={}], cost={}ms", sourceName, size, System.currentTimeMillis() - batchTime);
					logProgress(sourceName, lineIndex, false);
					batchData.clear();
					batchTime = System.currentTimeMillis();
				}
			}
			if(!batchData.isEmpty()){
				checkInterrupt();
				int size = batchData.size();
				batchProcess(batchData, batchTime);  
				log.info("finish batch[file={}, size={}], cost={}ms", sourceName, size, System.currentTimeMillis() - batchTime);
				logProgress(sourceName, lineIndex, false);
			}

			E e = onTextComplete(sourceUri, sourceName);
			logProgress(sourceName, lineIndex, true);
			return e;
		}finally{
			IoUtil.close(reader);
		}
	}

	/**
	 * 将行字段数据映射成对应的bean或者map
	 * @param row row
	 * @param batchTime 批处理时间
	 * @return 映射结果V列表
	 * @throws Exception Exception
	 */
	protected abstract List<V> parseRowData(IRow row, long batchTime) throws Exception;

	/**
	 * 批处理行数据
	 * @param batchData batchData
	 * @param batchTime batchTime
	 * @throws Exception Exception
	 */
	protected abstract void batchProcess(List<V> batchData, long batchTime) throws Exception;

	/**
	 * 单个text文件解析完成时的动作
	 * @param sourceUri sourceUri
	 * @param sourceName sourceName
	 */
	protected abstract E onTextComplete(String sourceUri, String sourceName) throws Exception;

	@Override
	protected void afterExec(E execResult) throws Exception {
		if(!(deleteSource(id) && deleteProgressLog())){
			log.warn("clean failed.");
		}
	}
}
