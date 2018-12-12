package com.fom.modules.importer.demo.wa;

import org.dom4j.Element;

import com.fom.context.ZipImporterConfig;
import com.fom.util.XmlUtil;

public class WAConfig extends ZipImporterConfig {

	private String zkAddress;

	private String fullTextUrl;
	
	protected WAConfig(String name) {
		super(name);
	}
	
	public String getZkAddress() {
		return zkAddress;
	}

	public String getFullTextUrl() {
		return fullTextUrl;
	}

	@Override
	protected void load(Element element) throws Exception {
		if(element == null){
			return;
		}
		zkAddress = XmlUtil.getString(element, "zkAddress", "");
		fullTextUrl = XmlUtil.getString(element, "fullTextUrl", "");
	}

}
