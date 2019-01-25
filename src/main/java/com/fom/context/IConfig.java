package com.fom.context;

/**
 * 
 * @author shanhm
 *
 */
public interface IConfig {

	String TYPE_IMPORTER = "importer";

	String TYPE_UPLOADER = "uploader";

	String TYPE_DOWNLOADER = "downloader";
	
	/**
	 * 当前模块类型（TYPE_IMPORTER/TYPE_UPLOADER/TYPE_DOWNLOADER）
	 * @return
	 */
	String getType();

	/**
	 * 匹配文件名称
	 * @param sourceName
	 * @return
	 */
	boolean matchSourceName(String sourceName);
	
	/**
	 * 当前模块扫描线程匹配源文件名称失败时是否删除
	 * @return
	 */
	boolean isDelMatchFailFile();
	
}
