package com.fom.examples;

import com.fom.context.Context;
import com.fom.context.executor.LocalZipImporter;

/**
 * 
 * @author shanhm
 *
 */
public class Example5 extends Context<TextZipImporterConfig>{

	protected Example5(String name, String path) {
		super(name, path);
	}

	@Override
	protected void exec(TextZipImporterConfig config) throws Exception { 
		Example5Helper helper = new Example5Helper(name, config.getEntryPattern());
		LocalZipImporter importer = new LocalZipImporter(name, sourceUri, config.getBatch(), helper);
		importer.exec();
	}
}
