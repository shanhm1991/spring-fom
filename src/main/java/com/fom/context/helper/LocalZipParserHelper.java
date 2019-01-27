package com.fom.context.helper;

/**
 * 
 * @author shanhm
 *
 * @param <V>
 */
public interface LocalZipParserHelper<V> extends ParserHelper<V> {

	/**
	 * 匹配zip的entry名称
	 * @param entryName
	 * @return
	 */
	boolean matchEntryName(String entryName);
}
