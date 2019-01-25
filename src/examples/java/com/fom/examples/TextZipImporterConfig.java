package com.fom.examples;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.Config;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class TextZipImporterConfig extends Config {
	
	private int batch;
	
	private Pattern pattern;

	protected TextZipImporterConfig(String name) {
		super(name);
	}
	
	@Override
	protected void loadExtends() throws Exception {
		batch = loadExtends("importer.batch", 5000, 1, 50000);
		String reg = loadExtends("zip.entryPattern", "");
		if(!StringUtils.isBlank(reg)){
			pattern = Pattern.compile(reg);
		}
	}

	@Override
	public String getType() {
		return TYPE_IMPORTER;
	}
	
	public int getBatch() {
		return batch;
	}
	
	public Pattern getEntryPattern(){
		return pattern;
	}
	
}
