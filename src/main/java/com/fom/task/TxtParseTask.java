package com.fom.task;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.ResultHandler;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.util.IoUtil;

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
 * @param <V> 行数据解析结果类型
 * 
 * @author shanhm
 *
 */
public abstract class TxtParseTask<V> extends ParseTask<V> {

	private int rowIndex = 0;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 */
	public TxtParseTask(String sourceUri, int batch){
		super(sourceUri, batch);
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param exceptionHandler ExceptionHandler
	 */
	public TxtParseTask(String sourceUri, int batch, ExceptionHandler exceptionHandler) {
		this(sourceUri, batch);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param resultHandler ResultHandler
	 */
	public TxtParseTask(String sourceUri, int batch, ResultHandler resultHandler) {
		this(sourceUri, batch);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public TxtParseTask(String sourceUri, int batch, 
			ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(sourceUri, batch);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	protected boolean beforeExec() throws Exception { 
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
		parseTxt(id, getSourceName(id), rowIndex);
		log.info("finish file(" 
				+ formatSize(getSourceSize(id)) + "KB), cost=" + (System.currentTimeMillis() - sTime) + "ms");
		return true;
	}

	protected final void parseTxt(String sourceUri, String sourceName, int lineIndex) throws Exception {
		Reader reader = null;
		RowData rowData = null;
		long batchTime = System.currentTimeMillis();
		try{
			reader = getSourceReader(sourceUri); 
			List<V> batchData = new LinkedList<>(); 
			while ((rowData = reader.readRow()) != null) {
				if(lineIndex > 0 && rowData.getRowIndex() <= lineIndex){
					continue;
				}
				lineIndex = rowData.getRowIndex();
				if (log.isDebugEnabled()) {
					log.debug("parse row[file=" 
							+ sourceName + ",rowIndex= " + rowIndex + "],columns=" + rowData.getColumnList());
				}

				List<V> dataList = parseRowData(rowData, batchTime);
				if(dataList != null){
					batchData.addAll(dataList);
				}

				if(batch > 0 && batchData.size() >= batch){
					checkInterrupt();
					int size = batchData.size();
					batchProcess(batchData, batchTime); 
					logProgress(sourceName, lineIndex, false);
					log.info("finish batch[file=" + sourceName 
							+ ",size=" + size + "],cost=" + (System.currentTimeMillis() - batchTime) + "ms");
					batchData.clear();
					batchTime = System.currentTimeMillis();
				}
			}
			if(!batchData.isEmpty()){
				checkInterrupt();
				int size = batchData.size();
				batchProcess(batchData, batchTime);  
				logProgress(sourceName, lineIndex, true);
				log.info("finish batch[file=" + sourceName 
						+ ",size=" + size + "],cost=" + (System.currentTimeMillis() - batchTime) + "ms");
			}
		}finally{
			IoUtil.close(reader);
		}
	}

	@Override
	protected boolean afterExec() throws Exception {
		return deleteSource(id) && deleteProgressLog();
	}
}
