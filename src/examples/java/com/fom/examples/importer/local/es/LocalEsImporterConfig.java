package com.fom.examples.importer.local.es;

import java.io.File;

import org.dom4j.Element;

import com.fom.context.ContextUtil;
import com.fom.context.config.Config;
import com.fom.context.executor.IImporterConfig;
import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public class LocalEsImporterConfig extends Config implements IImporterConfig {
	
	private int batch;
	
	private String esIndex;
	
	private String esType;
	
	private String esJson;
	
	private File esJsonFile;
	
	protected LocalEsImporterConfig(String name) {
		super(name);
	}
	
	@Override
	public String getType() {
		return TYPE_IMPORTER;
	}

	@Override
	public String getTypeName() {
		return TYPENAME_IMPORTER;
	}
	
	@Override
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

	
	@Override
	protected void load(Element e) throws Exception {
		batch = XmlUtil.getInt(e, "importer.batch", 5000, 1, 50000);
		esIndex = XmlUtil.getString(e, "es.index", ""); 
		esType = XmlUtil.getString(e, "es.type", ""); 
		esJson = XmlUtil.getString(e, "es.json", ""); 
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
	public boolean equals(Object o) {
		if(!(o instanceof LocalEsImporterConfig)){
			return false;
		}
		if(o == this){
			return true;
		}

		LocalEsImporterConfig config = (LocalEsImporterConfig)o;
		boolean equal = super.equals(config);
		if(!equal){
			return false;
		}
		
		return esIndex.equals(config.esIndex)
				&& esType.equals(config.esType)
				&& esJson.equals(config.esJson); 
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nes.index=" + esIndex);
		builder.append("\nes.type=" + esType);
		builder.append("\nes.json=" + esJson);
		return builder.toString();
	}

}
