package com.fom.context.executor.helper.importer;

/**
 * 
 * @author shanhm
 * @date 2019年1月23日
 *
 * @param <V>
 */
public abstract class LocalZipImporterHelper<V> extends abstractImporterHelper<V> {

	public LocalZipImporterHelper(String name) {
		super(name);
	}

	public abstract boolean matchZipSubFile(String name);
	
}
