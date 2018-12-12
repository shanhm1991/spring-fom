package com.fom.modules.importer.demo.hm;

import java.io.File;

import org.dom4j.Element;

import com.fom.context.ZipImporterConfig;
import com.fom.util.XmlUtil;

public class HMConfig extends ZipImporterConfig {
	
	private String zkAddress;

	private String esIndex;

	private String esType;
	
	private String esJson;
	
	private File esJsonFile;

	protected HMConfig(String name) {
		super(name);
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
	
}
