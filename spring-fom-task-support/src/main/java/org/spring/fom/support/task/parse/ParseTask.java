package org.spring.fom.support.task.parse;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;
import org.springframework.fom.Task;

/**
 * 
 * @author shanhm1991@163.com
 *
 * @param <V> 解析任务行数据解析结果类型
 * @param <E> 任务执行结果类型
 */
public abstract class ParseTask<V, E> extends Task<E> {
	
	public static final float FILE_UNIT = 1024.0f;
	
	protected final DecimalFormat sizeFormat = new DecimalFormat("#.###");
	
	protected final String parseCache;
	
	protected final int batch;
	
	protected File progressLog;
	
	/**
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 */
	public ParseTask(String sourceUri, int batch){
		super(sourceUri);
		this.batch = batch;
		if(StringUtils.isBlank(System.getProperty("cache.parse"))){ 
			parseCache = "cache/parse";
		}else{
			parseCache = System.getProperty("cache.parse");
		}
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
			logger.error("delete progress log failed.");
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
