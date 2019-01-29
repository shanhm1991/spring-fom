package com.fom.examples;

import java.io.File;

import com.fom.context.executor.Parser;
import com.fom.db.handler.EsHandler;

public class ImportEsExampleExecutor extends Parser {
	
	private static final String POOL = "example_es";
	
	private String esIndex;
	
	private String esType;
	
	private File esJson;

	public ImportEsExampleExecutor(String sourceUri, int batch, 
			ImportEsExampleHelper helper, String esIndex, String esType, File esJson) {
		super(sourceUri, batch, helper); 
		this.esIndex = esIndex;
		this.esType = esType;
		this.esJson = esJson;
	}

	@Override
	protected boolean onStart() throws Exception {
		super.onStart();
		if(EsHandler.handler.synCreateIndex(POOL, esIndex, esType, esJson)){
			log.info("创建ES索引[index=" + "demo" + ", type=" + "demo" + "]");
		}
		return true;
	}

}
