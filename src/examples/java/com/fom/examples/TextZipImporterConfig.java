package com.fom.examples;

import com.fom.context.Config;

/**
 * 
 * @author shanhm
 *
 */
public class TextZipImporterConfig extends Config {
	
	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;
	
	private String pattern;

	protected TextZipImporterConfig(String name) {
		super(name);
	}
	
	@Override
	protected void loadExtends() throws Exception {
		batch = load("importer.batch", 5000, 1, 50000);
		srcPath = load("src.path", "");
		isDelMatchFail = load("importer.isDelMatchFail", false);
		pattern = load("zip.entryPattern", "");
	}

	public int getBatch() {
		return batch;
	}

	public String getSrcPath() {
		return srcPath;
	}

	public boolean isDelMatchFail() {
		return isDelMatchFail;
	}
	
	public String getEntryPattern(){
		return pattern;
	}
	
}
