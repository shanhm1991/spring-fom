package com.fom.task;

import java.io.File;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.ResultHandler;
import com.fom.task.helper.TxtParseHelper;
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
 * @see TxtParseHelper
 * 
 * @param <V> 行数据解析结果类型
 * 
 * @author shanhm
 *
 */
public abstract class TxtParseTask<V> extends ParseTask<V> {

	private TxtParseHelper helper;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper TxtParseHelper
	 */
	public TxtParseTask(String sourceUri, int batch, TxtParseHelper helper){
		super(sourceUri, batch, helper);
		this.helper = helper;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	public TxtParseTask(String sourceUri, int batch, TxtParseHelper helper, ExceptionHandler exceptionHandler) {
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 * @param resultHandler ResultHandler
	 */
	public TxtParseTask(String sourceUri, int batch, TxtParseHelper helper, ResultHandler resultHandler) {
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
	public TxtParseTask(String sourceUri, int batch, 
			TxtParseHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(sourceUri, batch, helper);
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
		parseTxt(id);
		String size = new DecimalFormat("#.###").format(helper.getSourceSize(id) / 1024.0);
		log.info("finish file(" + size + "KB), cost=" + (System.currentTimeMillis() - sTime) + "ms");
		return true;
	}

	protected final void parseTxt(String sourceUri, int rowIndex) throws Exception {
		Reader reader = null;
		RowData rowData = null;
		String name = new File(sourceUri).getName();
		long batchTime = System.currentTimeMillis();
		try{
			reader = helper.getReader(sourceUri);
			List<V> batchData = new LinkedList<>(); 
			while ((rowData = reader.readRow()) != null) {
				if(rowIndex > 0 && rowData.getRowIndex() <= rowIndex){
					continue;
				}
				rowIndex = rowData.getRowIndex();

				if (log.isDebugEnabled()) {
					log.debug("parse row[rowIndex= " + rowIndex + "],columns=" + rowData.getColumnList());
				}
				List<V> dataList = parseRowData(rowData, batchTime);
				if(dataList != null){
					batchData.addAll(dataList);
				}

				if(batch > 0 && batchData.size() >= batch){
					TaskUtil.checkInterrupt();
					int size = batchData.size();
					batchProcess(batchData, batchTime); 
					TaskUtil.log(log, progressLog, rowIndex);
					
					batchData.clear();
					batchTime = System.currentTimeMillis();
					log.info("finish batch[size=" + size + "],cost=" + (System.currentTimeMillis() - batchTime) + "ms");
				}
			}
			if(!batchData.isEmpty()){
				TaskUtil.checkInterrupt();
				int size = batchData.size();
				batchProcess(batchData, batchTime);  
				TaskUtil.log(log, progressLog, rowIndex);
				log.info("finish batch[size=" + size + "],cost=" + (System.currentTimeMillis() - batchTime) + "ms");
			}
		}finally{
			IoUtil.close(reader);
		}
	}
	
	@Override
	protected boolean afterExec() throws Exception {
		if(!helper.delete(id)){ 
			log.error("delete src file failed.");
			return false;
		}
		if(progressLog.exists() && !progressLog.delete()){
			log.error("delete progress log failed.");
			return false;
		}
		return true;
	}
}
