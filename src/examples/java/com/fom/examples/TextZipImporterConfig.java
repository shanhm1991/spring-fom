package com.fom.examples;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.Config;
import com.fom.context.executor.LocalZipImporterConfig;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class TextZipImporterConfig extends Config implements LocalZipImporterConfig {
	
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
	public boolean matchEntryName(String fileName) {
		if(pattern == null){
			return true;
		}
		return pattern.matcher(fileName).find();
	}

	@Override
	public String getType() {
		return TYPE_IMPORTER;
	}
	
	@Override
	public int getBatch() {
		return batch;
	}
	
}
