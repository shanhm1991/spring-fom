package com.fom.examples;

import com.fom.context.Context;
import com.fom.context.executor.LocalZipParser;

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
		LocalZipParser importer = new LocalZipParser(name, sourceUri, config.getBatch(), helper);
		importer.exec();
	}
}
