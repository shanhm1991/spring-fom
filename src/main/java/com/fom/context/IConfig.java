package com.fom.context;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public interface IConfig {

	String TYPE_IMPORTER = "importer";

	String TYPE_UPLOADER = "uploader";

	String TYPE_DOWNLOADER = "downloader";

	String TYPENAME_IMPORTER = "导入";

	String TYPENAME_UPLOADER = "上传";

	String TYPENAME_DOWNLOADER = "下载";
	
	/**
	 * 当前模块类型（importer/uploader/downloader）
	 * @return
	 */
	String getType();

	/**
	 * 当前模块类型名称(导入/上传/下载)
	 * @return
	 */
	String getTypeName();
	
	/**
	 * 当前模块是否在运行
	 * @return
	 */
	boolean isRunning();
	
	/**
	 * 当前模块的执行器
	 * @return
	 */
	String getExecutorClass();
	
	/**
	 * 当前模块扫描线程等待下一次执行的时间
	 * @return
	 */
	long nextScanTime();

	/**
	 * 当前模块扫描线程扫描源文件时的名称匹配
	 * @param name
	 * @return
	 */
	boolean matchSrc(String name);
	
	/**
	 * 当前模块扫描线程匹配源文件名称失败时是否删除
	 * @return
	 */
	boolean isDelMatchFailFile();
	
	/**
	 * 扫描源文件的uri
	 * @return
	 */
	String getUri();

	/**
	 * 当前模块执行线程数的最小数
	 * @return
	 */
	int getExecutorMin();

	/**
	 * 当前模块执行线程数的最大数
	 * @return
	 */
	int getExecutorMax();

	/**
	 * 当前模块执行线程的最长空闲时间
	 * @return
	 */
	int getExecutorAliveTime();

	/**
	 * 当前模块执行线程的超时时间 
	 * @return
	 */
	int getExecutorOverTime();

	/**
	 * 当前模块执行线程的如果超时是否中断
	 * @return
	 */
	boolean getInterruptOnOverTime();
}
