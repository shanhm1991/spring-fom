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
public class LocalOraclePoolZipImporter extends Context<LocalZipImporterConfig>{

	protected LocalOraclePoolZipImporter(String name, String path) {
		super(name, path);
	}

	@Override
	protected void exec(LocalZipImporterConfig config) throws Exception {
		LocalOraclePoolZipImporterHelper helper = new LocalOraclePoolZipImporterHelper(name);
		Executor executor = new Importer(name, sourceUri, config, helper);
		executor.exec();
	}
}
