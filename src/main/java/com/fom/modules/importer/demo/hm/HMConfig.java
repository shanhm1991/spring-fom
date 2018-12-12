package com.fom.modules.importer.demo.hm;

import java.io.File;

import org.dom4j.Element;

import com.fom.context.ZipImporterConfig;
import com.fom.util.XmlUtil;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
public class HMConfig extends ZipImporterConfig {
	
	private String zkAddress;

	private String esIndex;

	private String esType;
	
	private String esJson;
	
	private File esJsonFile;

	protected HMConfig(String name) {
		super(name);
	}

	@Override
	protected void load(Element element) throws Exception {
		if(element == null){
			return;
		}
		zkAddress = XmlUtil.getString(element, "zkAddress", "");
		esIndex = XmlUtil.getString(element, "es.index", "");
		esType = XmlUtil.getString(element, "es.type", "");
		esJson = XmlUtil.getString(element, "es.json", "");
		esJsonFile = locationResource(esJson);
	}
	
	protected boolean valid(Element extendedElement) throws Exception {
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nzkAddress=" + zkAddress);
		builder.append("\nes.index=" + esIndex);
		builder.append("\nes.type=" + esType);
		builder.append("\nes.json=" + esJson);
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof HMConfig)){
			return false;
		}
		if(o == this){
			return true;
		}
		
		HMConfig c = (HMConfig)o; 
		if(!super.equals(c)){
			return false;
		}
		
		return zkAddress.equals(c.zkAddress)
				&& esIndex.equals(c.esIndex)
				&& esType.equals(c.esType)
				&& esJson.equals(c.esJson);
	}
	
	public String getZkAddress() {
		return zkAddress;
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
