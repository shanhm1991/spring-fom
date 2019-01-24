package com.fom.context.executor;


/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public interface LocalZipImporterConfig extends ImporterConfig {
	
	/**
	 * 通过名称匹配zip子文件是否需要处理
	 * @param fileName
	 * @return
	 */
	boolean matchSubFile(String fileName);

}
