package com.fom.examples.importer.local.oracle;

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
public class LocalTextZipImporterConfig extends Config implements LocalZipImporterConfig {
	
	private int batch;
	
	private Pattern pattern;

	protected LocalTextZipImporterConfig(String name) {
		super(name);
	}
	
	@Override
	protected void loadExtends() throws Exception {
		batch = loadExtends("importer.batch", 5000, 1, 50000);
		String reg = loadExtends("zip.subPattern", "");
		if(!StringUtils.isBlank(reg)){
			pattern = Pattern.compile(reg);
		}
	}
	
	@Override
	public boolean matchSubFile(String fileName) {
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
	public String getTypeName() {
		return TYPENAME_IMPORTER;
	}
	
	@Override
	public int getBatch() {
		return batch;
	}
	
}
