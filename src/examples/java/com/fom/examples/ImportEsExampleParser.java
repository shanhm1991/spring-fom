package com.fom.examples;

import java.io.File;

import com.fom.context.task.ParseTask;
import com.fom.db.handler.EsHandler;

public class ImportEsExampleParser extends ParseTask {
	
	private static final String POOL = "example_es";
	
	private String esIndex;
	
	private String esType;
	
	private File esJson;

	public ImportEsExampleParser(String sourceUri, int batch, 
			ImportEsExampleHelper helper, String esIndex, String esType, File esJson) {
		super(sourceUri, batch, helper); 
		this.esIndex = esIndex;
		this.esType = esType;
		this.esJson = esJson;
	}

	@Override
	protected boolean beforeExec() throws Exception {
		super.beforeExec();
		if(EsHandler.handler.synCreateIndex(POOL, esIndex, esType, esJson)){
			log.info("创建ES索引[index=" + "demo" + ", type=" + "demo" + "]");
		}
		return true;
	}

}
