package com.fom.examples;

import com.fom.context.Context;
import com.fom.context.executor.LocalZipImporter;

/**
 * 	
 * @author shanhm
 *
 */
public class Example4 extends Context<TextZipImporterConfig>{

	protected Example4(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void exec(TextZipImporterConfig config) throws Exception {
		Example4Helper helper = new Example4Helper(name, config.getEntryPattern());
		LocalZipImporter importer = new LocalZipImporter(name, sourceUri, config.getBatch(), helper);
		importer.exec();
	}
}
