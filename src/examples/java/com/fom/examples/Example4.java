package com.fom.examples;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.executor.Importer;

/**
 * 	
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class Example4 extends Context<TextZipImporterConfig>{

	protected Example4(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void exec(TextZipImporterConfig config) throws Exception {
		Example4Helper helper = new Example4Helper(name);
		Executor executor = new Importer(name, sourceUri, config, helper);
		executor.exec();
	}
}
