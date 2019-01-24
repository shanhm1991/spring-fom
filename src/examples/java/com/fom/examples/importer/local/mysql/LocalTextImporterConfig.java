package com.fom.examples.importer.local.mysql;

import com.fom.context.Config;
import com.fom.context.executor.ImporterConfig;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalTextImporterConfig extends Config implements ImporterConfig {
	
	private int batch;

	protected LocalTextImporterConfig(String name) {
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

	@Override
	public String getTypeName() {
		return TYPENAME_IMPORTER;
	}

	@Override
	public int getBatch() {
		return batch;
	}
	
}
