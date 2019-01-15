package com.fom.examples.importer.local.es;

import java.io.File;

import org.dom4j.Element;

import com.fom.context.ContextUtil;
import com.fom.context.config.ImporterConfig;
import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public class LocalEsImporterConfig extends ImporterConfig {
	
	private String esIndex;
	
	private String esType;
	
	private String esJson;
	
	private File esJsonFile;
	

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

	protected LocalEsImporterConfig(String name) {
		super(name);
	}
	
	/**
	 * 继承自Config，自定义加载<extended>中的配置项
	 */
	@Override
	protected void load(Element extendedElement) throws Exception {
		esIndex = XmlUtil.getString(extendedElement, "es.index", ""); 
		esType = XmlUtil.getString(extendedElement, "es.type", ""); 
		esJson = XmlUtil.getString(extendedElement, "es.json", ""); 
	}

	/**
	 * 继承自Config，自定义校验<extended>中的配置项
	 */
	@Override
	protected boolean valid() throws Exception {
		esJsonFile = new File(ContextUtil.getRealPath(esJson));
		if(!esJsonFile.exists()){
			LOG.error("文件不存在：" + esJson); 
			return false;
		}
		return true;
	}
	
	/**
	 * 需要继承父类复写，在打日志的时候以及页面展示的时候即调用的toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nes.index=" + esIndex);
		builder.append("\nes.type=" + esType);
		builder.append("\nes.json=" + esJson);
		return builder.toString();
	}
	
	/**
	 * 需要继承父类复写，再修改配置时判断config配置项有没有变化时即调用的equals(Object o)
	 */
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
}
