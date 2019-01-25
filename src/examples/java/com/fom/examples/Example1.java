package com.fom.examples;

import com.fom.context.Context;
import com.fom.context.executor.Importer;
import com.fom.db.handler.EsHandler;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public class Example1 extends Context<EsImpoterConfig> {
	
	private static final String POOL = "example_es";
	
	protected Example1(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void onStart(EsImpoterConfig config) throws Exception {
		if(EsHandler.handler.synCreateIndex(
				POOL, config.getEsIndex(), config.getEsType(), config.getEsJsonFile())){
			log.info("创建ES索引[index=" + "demo" + ", type=" + "demo" + "]");
		}
	}
	
	@Override
	protected void exec(EsImpoterConfig config) throws Exception {
		Example1Helper helper = new Example1Helper(name, config.getEsIndex(), config.getEsType()); 
		Importer importer = new Importer(name, sourceUri, config, helper);
		importer.exec();
	}
}
