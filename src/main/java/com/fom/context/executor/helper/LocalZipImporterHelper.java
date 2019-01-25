package com.fom.context.executor.helper;

public interface LocalZipImporterHelper<V> extends ImporterHelper<V> {

	/**
	 * 匹配zip的entry名称
	 * @param entryName
	 * @return
	 */
	boolean matchEntryName(String entryName);
}
