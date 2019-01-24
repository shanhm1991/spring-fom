package com.fom.examples.importer.local.mysql;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.executor.Importer;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalMysqlPoolImporter extends Context<LocalTextImporterConfig> {
	
	protected LocalMysqlPoolImporter(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void exec(LocalTextImporterConfig config) throws Exception {
		LocalMysqlPoolImporterHelper helper = new LocalMysqlPoolImporterHelper(name);
		Executor executor = new Importer(name, sourceUri, config, helper);
		executor.exec();
	}
}
