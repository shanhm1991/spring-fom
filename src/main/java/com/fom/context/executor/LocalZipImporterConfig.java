package com.fom.context.executor;


/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public interface LocalZipImporterConfig extends ImporterConfig {
	
	/**
	 * 匹配zip的entry名称
	 * @param entryName
	 * @return
	 */
	boolean matchEntryName(String entryName);

}
