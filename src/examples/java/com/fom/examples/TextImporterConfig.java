package com.fom.examples;

import com.fom.context.Config;

/**
 * 
 * @author shanhm
 *
 */
public class TextImporterConfig extends Config {
	
	private int batch;

	protected TextImporterConfig(String name) {
		super(name);
	}
	
	@Override
	protected void loadExtends() throws Exception {
		batch = loadExtends("importer.batch", 5000, 1, 50000);
	}

	@Override
	public String getType() {
		return TYPE_IMPORTER;
	}

	public int getBatch() {
		return batch;
	}
	
}
