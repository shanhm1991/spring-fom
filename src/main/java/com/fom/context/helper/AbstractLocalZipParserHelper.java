package com.fom.context.helper;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author shanhm
 *
 * @param <V>
 */
public abstract class AbstractLocalZipParserHelper<V> extends AbstractParserHelper<V> implements LocalZipParserHelper<V> {

	private Pattern pattern;
	
	public AbstractLocalZipParserHelper(String name, Pattern pattern) {
		super(name);
		this.pattern = pattern;
	}
	
	public AbstractLocalZipParserHelper(String name, String pattern) {
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
