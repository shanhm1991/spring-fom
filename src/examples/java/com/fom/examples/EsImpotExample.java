package com.fom.examples;

import java.io.File;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.executor.Parser;
import com.fom.db.handler.EsHandler;
import com.fom.util.ScanUtil;

/**
 * 
 * @author shanhm
 *
 */
public class EsImpotExample extends Context<EsImpotExampleConfig> {
	
	private static final String POOL = "example_es";

	@Override
	protected List<String> getUriList(EsImpotExampleConfig config) throws Exception {
		return ScanUtil.scan(config.getSrcPath(), config.getPattern(), config.isDelMatchFail());
	}

	@Override
	protected Executor createExecutor(String sourceUri, EsImpotExampleConfig config) throws Exception {
		EsImpotExampleHelper helper = new EsImpotExampleHelper(name, config.getEsIndex(), config.getEsType()); 
		
		EsImpotExampleExecutor executor = new EsImpotExampleExecutor(name,sourceUri,sourceUri
				
				);
		
		return executor;
	}
	
	

//	
//	@Override
//	protected void exec(EsImpotExampleConfig config) throws Exception {
//		Example1Helper helper = new Example1Helper(name, config.getEsIndex(), config.getEsType()); 
//		Parser importer = new Parser(name, sourceUri, config.getBatch(), helper);
//		importer.exec();
//	}
}
