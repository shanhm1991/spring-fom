package org.eto.fom.task.parse;

import java.io.File;
import java.text.DecimalFormat;

import org.eto.fom.context.core.Task;

/**
 * 
 * @author shanhm
 *
 * @param <V> 行数据解析结果类型
 */
public abstract class ParseTask<V, E> extends Task<E> {
	
	protected final int batch;
	
	protected final DecimalFormat sizeFormat = new DecimalFormat("#.###");
	
	protected File progressLog;
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 */
	public ParseTask(String sourceUri, int batch){
		super(sourceUri);
		this.batch = batch;
	}
	
	/**
	 * 获取对应sourceUri的资源名称
	 * @param sourceUri 资源uri
	 * @return 资源名称
	 */
	protected String getSourceName(String sourceUri) {
		return new File(sourceUri).getName();
	}
	
	/**
	 * 获取对应sourceUri的资源字节数
	 * @param sourceUri 资源uri
	 * @return 资源字节数
	 */
	protected long getSourceSize(String sourceUri) {
		return new File(sourceUri).length();
	}
	
	/**
	 * 根据sourceUri删除文件
	 * @param sourceUri 资源uri
	 * @return 是否删除成功
	 */
	protected boolean deleteSource(String sourceUri) {
		return new File(sourceUri).delete();
	}
	
	/**
	 * 删除处理进度纪录日志
	 * @return 是否删除成功
	 */
	protected final boolean deleteProgressLog() {
		if(progressLog.exists() && !progressLog.delete()){
			log.error("delete progress log failed.");
			return false;
		}
		return true;
	}
	
	/**
	 * 文件大小格式化
	 * @param size size
	 * @return KB
	 */
	protected final String formatSize(long size) {
		return sizeFormat.format(size / FILE_UNIT);
	}
	
	/**
	 * 中断检测
	 * @throws InterruptedException InterruptedException
	 */
	protected final void checkInterrupt() throws InterruptedException{
		if(Thread.interrupted()){
			throw new InterruptedException("interrupted when batchProcessLineData");
		}
	}
	
}
