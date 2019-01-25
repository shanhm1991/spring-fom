package com.fom.context.executor.helper;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author shanhm
 *
 * @param <V>
 */
public abstract class AbstractLocalZipImporterHelper<V> extends AbstractImporterHelper<V> implements LocalZipImporterHelper<V> {

	private Pattern pattern;
	
	public AbstractLocalZipImporterHelper(String name, Pattern pattern) {
		super(name);
		this.pattern = pattern;
	}
	
	public AbstractLocalZipImporterHelper(String name, String pattern) {
		super(name); 
		if(!StringUtils.isBlank(pattern)){
			this.pattern = Pattern.compile(pattern);
		}
	}

	@Override
	public final boolean matchEntryName(String entryName) {
		if(pattern == null){
			return true;
		}
		return pattern.matcher(entryName).find();
	}
}
