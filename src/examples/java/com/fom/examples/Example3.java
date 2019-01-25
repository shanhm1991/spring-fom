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
public class Example3 extends Context<TextImporterConfig> {
	
	protected Example3(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void exec(TextImporterConfig config) throws Exception {
		Example3Helper helper = new Example3Helper(name);
		Executor executor = new Importer(name, sourceUri, config, helper);
		executor.exec();
	}
}
