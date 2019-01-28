package com.fom.examples;

import java.io.File;

import com.fom.context.Config;
import com.fom.context.ContextManager;

/**
 * 
 * @author shanhm
 *
 */
public class ImportEsExampleConfig extends Config {
	
	private String srcPath;
	
	private int batch;
	
	private boolean isDelMatchFail;
	
	private String esIndex;
	
	private String esType;
	
	private String esJson;
	
	private File esJsonFile;
	
	protected ImportEsExampleConfig(String name) {
		super(name);
	}
	
	@Override
	protected void loadExtends() throws Exception {
		srcPath = load("src.path", "");
		isDelMatchFail = load("importer.isDelMatchFail", false);
		batch = load("importer.batch", 5000, 1, 50000);
		esIndex = load("es.index", ""); 
		esType = load("es.type", ""); 
		esJson = load("es.json", ""); 
	}
	
	@Override
	protected boolean valid() throws Exception {
		esJsonFile = new File(ContextManager.getContextPath(esJson));
		if(!esJsonFile.exists()){
			LOG.error("文件不存在：" + esJson); 
			return false;
		}
		return true;
	}
	
	public String getSrcPath() {
		return srcPath;
	}
	
	public boolean isDelMatchFail() {
		return isDelMatchFail;
	}

	public int getBatch() {
		return batch;
	}

	public String getEsIndex() {
		return esIndex;
	}

	public String getEsType() {
		return esType;
	}

	public String getEsJson() {
		return esJson;
	}

	public File getEsJsonFile() {
		return esJsonFile;
	}

}
