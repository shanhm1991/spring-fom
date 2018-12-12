package com.fom.modules.importer.demo.st;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

import com.fom.context.ZipImporterConfig;
import com.fom.util.XmlUtil;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
public class STConfig extends ZipImporterConfig {

	private String userIndex;

	private String userType;

	private String userJson;
	
	private File userJsonFile;
	
	private String groupIndex;

	private String groupType;

	private String groupJson;
	
	private File groupJsonFile;
	
	private String gzhIndex;

	private String gzhType;

	private String gzhJson;
	
	private File gzhJsonFile;
	
	private String zkAddress;
	
	protected STConfig(String name) {
		super(name);
	}

	@Override
	protected void load(Element element) throws Exception {
		if(element == null){
			return;
		}
		zkAddress = XmlUtil.getString(element, "zkAddress", "");
		userIndex = XmlUtil.getString(element, "es.index.user", "");
		userType = XmlUtil.getString(element, "es.type.user", "");
		userJson = XmlUtil.getString(element, "es.json.user", "");
		groupIndex = XmlUtil.getString(element, "es.index.group", "");
		groupType = XmlUtil.getString(element, "es.type.group", "");
		groupJson = XmlUtil.getString(element, "es.json.group", "");
		gzhIndex = XmlUtil.getString(element, "es.index.gzh", "");
		gzhType = XmlUtil.getString(element, "es.type.gzh", "");
		gzhJson = XmlUtil.getString(element, "es.json.gzh", "");
	}
	
	@Override
	protected boolean valid(Element extendedElement) throws Exception {
		if(StringUtils.isBlank(userIndex)){ 
			LOG.warn("缺少配置:es.index.user");
			return false;
		}
		if(StringUtils.isBlank(userType)){ 
			LOG.warn("缺少配置:es.type.user");
			return false;
		}
		if(StringUtils.isBlank(userJson)){ 
			LOG.warn("缺少配置:es.json.user");
			return false;
		}
		userJsonFile = locationResource(userJson);
		
		if(StringUtils.isBlank(groupIndex)){ 
			LOG.warn("缺少配置:es.index.group");
			return false;
		}
		if(StringUtils.isBlank(groupType)){ 
			LOG.warn("缺少配置:es.type.group");
			return false;
		}
		if(StringUtils.isBlank(groupJson)){ 
			LOG.warn("缺少配置:es.json.group");
			return false;
		}
		groupJsonFile = locationResource(groupJson);
		
		if(StringUtils.isBlank(gzhIndex)){ 
			LOG.warn("缺少配置:es.index.gzh");
			return false;
		}
		if(StringUtils.isBlank(gzhType)){ 
			LOG.warn("缺少配置:es.type.gzh");
			return false;
		}
		if(StringUtils.isBlank(gzhJson)){ 
			LOG.warn("缺少配置:es.json.gzh");
			return false;
		}
		gzhJsonFile = locationResource(gzhJson);
		
		if(StringUtils.isBlank(zkAddress)){ 
			LOG.warn("缺少配置:zkAddress");
			return false;
		}
		return true;
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nes.index.user=" + userIndex);
		builder.append("\nes.type.user=" + userType);
		builder.append("\nes.json.user=" + userJson);
		builder.append("\nes.index.group=" + groupIndex);
		builder.append("\nes.type.group=" + groupType);
		builder.append("\nes.json.group=" + groupJson);
		builder.append("\nes.index.gzh=" + gzhIndex);
		builder.append("\nes.type.gzh=" + gzhType);
		builder.append("\nes.json.gzh=" + gzhJson);
		builder.append("\nzkAddress=" + zkAddress);
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof STConfig)){
			return false;
		}
		if(o == this){
			return true;
		}

		STConfig config = (STConfig)o;
		boolean equal = super.equals(config);
		if(!equal){
			return false;
		}

		return userIndex.equals(config.userIndex) 
				&& userType.equals(config.userType) 
				&& userJson.equals(config.userJson)
				&& groupIndex.equals(config.groupIndex) 
				&& groupType.equals(config.groupType) 
				&& groupJson.equals(config.groupJson)
				&& gzhIndex.equals(config.gzhIndex) 
				&& gzhType.equals(config.gzhType) 
				&& gzhJson.equals(config.gzhJson)
				&& zkAddress.equals(config.zkAddress);
	}

	public String getZkAddress() {
		return zkAddress;
	}

	public String getUserIndex() {
		return userIndex;
	}

	public String getUserType() {
		return userType;
	}

	public String getUserJson() {
		return userJson;
	}

	public String getGroupIndex() {
		return groupIndex;
	}

	public String getGroupType() {
		return groupType;
	}

	public String getGroupJson() {
		return groupJson;
	}

	public String getGzhIndex() {
		return gzhIndex;
	}

	public String getGzhType() {
		return gzhType;
	}

	public String getGzhJson() {
		return gzhJson;
	}

	public File getUserJsonFile() {
		return userJsonFile;
	}

	public File getGroupJsonFile() {
		return groupJsonFile;
	}

	public File getGzhJsonFile() {
		return gzhJsonFile;
	}
	
}
