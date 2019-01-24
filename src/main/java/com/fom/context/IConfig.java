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
	 * 匹配文件名称
	 * @param srcName
	 * @return
	 */
	boolean matchSrc(String srcName);
	
	/**
	 * 当前模块扫描线程匹配源文件名称失败时是否删除
	 * @return
	 */
	boolean isDelMatchFailFile();
	
}
