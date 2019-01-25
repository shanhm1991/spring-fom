package com.fom.examples;

import com.fom.context.Context;
import com.fom.context.executor.Importer;

/**
 * 
 * @author shanhm
 *
 */
public class Example3 extends Context<TextImporterConfig> {
	
	protected Example3(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void exec(TextImporterConfig config) throws Exception {
		Example3Helper helper = new Example3Helper(name);
		Importer importer = new Importer(name, sourceUri, config.getBatch(), helper);
		importer.exec();
	}
}
