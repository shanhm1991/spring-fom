package com.fom.examples;

import java.io.File;

import com.fom.context.Config;
import com.fom.context.ContextUtil;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public class EsImpoterConfig extends Config {
	
	private int batch;
	
	private String esIndex;
	
	private String esType;
	
	private String esJson;
	
	private File esJsonFile;
	
	protected EsImpoterConfig(String name) {
		super(name);
	}
	
	@Override
	protected void loadExtends() throws Exception {
		batch = loadExtends("importer.batch", 5000, 1, 50000);
		esIndex = loadExtends("es.index", ""); 
		esType = loadExtends("es.type", ""); 
		esJson = loadExtends("es.json", ""); 
	}
	
	@Override
	protected boolean valid() throws Exception {
		esJsonFile = new File(ContextUtil.getRealPath(esJson));
		if(!esJsonFile.exists()){
			LOG.error("文件不存在：" + esJson); 
			return false;
		}
		return true;
	}
	
	@Override
	public String getType() {
		return TYPE_IMPORTER;
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
