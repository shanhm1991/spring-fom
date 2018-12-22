package com.fom.modules.importer.demo.wa;

import org.dom4j.Element;

import com.fom.context.ZipImporterConfig;
import com.fom.util.XmlUtil;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
public class WAConfig extends ZipImporterConfig {

	private String zkAddress;

	private String fullTextUrl;
	
	protected WAConfig(String name) {
		super(name);
	}
	
	@Override
	protected void load(Element element) throws Exception {
		if(element == null){
			return;
		}
		zkAddress = XmlUtil.getString(element, "zkAddress", "");
		fullTextUrl = XmlUtil.getString(element, "fullTextUrl", "");
	}
	
	@Override
	protected boolean valid(Element extendedElement) throws Exception {
		return true;
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nzkAddress=" + zkAddress);
		builder.append("\nfullTextUrl=" + fullTextUrl);
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof WAConfig)){
			return false;
		}
		if(o == this){
			return true;
		}

		WAConfig config = (WAConfig)o;
		boolean equal = super.equals(config);
		if(!equal){
			return false;
		}
		
		return zkAddress.equals(config.zkAddress)
				&& fullTextUrl.equals(config.fullTextUrl);
	}
	
	public String getZkAddress() {
		return zkAddress;
	}

	public String getFullTextUrl() {
		return fullTextUrl;
	}

}
