package com.fom.examples;

import com.fom.context.Config;

/**
 * 
 * @author shanhm
 *
 */
public class TextImporterConfig extends Config {

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;

	protected TextImporterConfig(String name) {
		super(name);
	}

	@Override
	protected void loadExtends() throws Exception { 
		batch = load("importer.batch", 5000, 1, 50000);
		srcPath = load("src.path", "");
		isDelMatchFail = load("importer.isDelMatchFail", false);
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
}
