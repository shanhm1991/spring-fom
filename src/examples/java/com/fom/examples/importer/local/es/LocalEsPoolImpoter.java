package com.fom.examples.importer.local.es;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.executor.Importer;
import com.fom.db.handler.EsHandler;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public class LocalEsPoolImpoter extends Context<LocalEsImporterConfig> {
	
	private static final String POOL = "example_es";
	
	protected LocalEsPoolImpoter(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void onStart(LocalEsImporterConfig config) throws Exception {
		if(EsHandler.handler.synCreateIndex(
				POOL, config.getEsIndex(), config.getEsType(), config.getEsJsonFile())){
			log.info("创建ES索引[index=" + "demo" + ", type=" + "demo" + "]");
		}
	}
	
	@Override
	protected void exec(LocalEsImporterConfig config) throws Exception {
		LocalEsPoolImpoterHelper helper = new LocalEsPoolImpoterHelper(name, config.getEsIndex(), config.getEsType()); 
		Executor executor = new Importer(name, sourceUri, config, helper);
		executor.exec();
	}
}
