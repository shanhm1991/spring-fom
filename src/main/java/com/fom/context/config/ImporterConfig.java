package com.fom.context.config;

import java.io.File;

import com.fom.util.XmlUtil;

/**
 * <importer.batch> 批处理行数
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class ImporterConfig extends Config {

	int batch;

	File progressDir = null;

	protected ImporterConfig(String name) {
		super(name);
	}

	@Override
	void load() throws Exception {
		super.load();
		batch = XmlUtil.getInt(element, "importer.batch", 5000, 1, 50000);
	}
	
	@Override
	boolean valid() throws Exception {
		if(!super.valid()){
			return false;
		}
		progressDir = new File(System.getProperty("import.progress") + File.separator + name);
		if(!progressDir.exists() && !progressDir.mkdirs()){
			LOG.error("创建处理目录失败:" + progressDir.getPath()); 
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nimporter.batch=" + batch);
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof ImporterConfig)){
			return false;
		}
		if(o == this){
			return true;
		}
		
		ImporterConfig c = (ImporterConfig)o; 
		if(!super.equals(c)){
			return false;
		}
		
		return batch == c.batch;
	}

	@Override
	public final String getType() {
		return TYPE_IMPORTER;
	}

	@Override
	public final String getTypeName() {
		return NAME_IMPORTER;
	}

	public int getBatch() {
		return batch;
	}
	
}
