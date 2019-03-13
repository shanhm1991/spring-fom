package com.fom.task;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.fom.context.Task;
import com.fom.task.helper.ParseHelper;
import com.fom.task.reader.RowData;

/**
 * 
 * @author shanhm
 *
 * @param <V> 行数据解析结果类型
 */
public abstract class ParseTask<V> extends Task {
	
	protected final int batch;
	
	protected final File progressLog;
	
	protected final ParseHelper helper;
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ParserHelper
	 */
	public ParseTask(String sourceUri, int batch, ParseHelper helper){
		super(sourceUri);
		this.batch = batch;
		this.helper = helper;
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
	}
	
	/**
	 * 将行字段数据映射成对应的bean或者map
	 * @param rowData
	 * @param batchTime 批处理时间
	 * @return 映射结果V列表
	 * @throws Exception Exception
	 */
	public abstract List<V> parseRowData(RowData rowData, long batchTime) throws Exception;

	/**
	 * 批处理行数据
	 * @param batchData batchData
	 * @param batchTime batchTime
	 * @throws Exception Exception
	 */
	public abstract void batchProcess(List<V> batchData, long batchTime) throws Exception;
	
}
