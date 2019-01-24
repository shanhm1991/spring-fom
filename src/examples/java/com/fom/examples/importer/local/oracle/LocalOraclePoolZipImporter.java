package com.fom.examples.importer.local.oracle;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.executor.Importer;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalOraclePoolZipImporter extends Context<LocalTextZipImporterConfig>{

	protected LocalOraclePoolZipImporter(String name, String path) {
		super(name, path);
	}

	@Override
	protected void exec(LocalTextZipImporterConfig config) throws Exception {
		LocalOraclePoolZipImporterHelper helper = new LocalOraclePoolZipImporterHelper(name);
		Executor executor = new Importer(name, sourceUri, config, helper);
		executor.exec();
	}
}
