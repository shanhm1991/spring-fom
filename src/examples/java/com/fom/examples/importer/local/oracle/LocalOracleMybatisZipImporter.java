package com.fom.examples.importer.local.oracle;

import com.fom.context.Context;
import com.fom.context.executor.Executor;
import com.fom.context.executor.Importer;

/**
 * 	
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalOracleMybatisZipImporter extends Context<LocalTextZipImporterConfig>{

	protected LocalOracleMybatisZipImporter(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void exec(LocalTextZipImporterConfig config) throws Exception {
		LocalOracleMybatisZipImporterHelper helper = new LocalOracleMybatisZipImporterHelper(name);
		Executor executor = new Importer(name, sourceUri, config, helper);
		executor.exec();
	}
}
