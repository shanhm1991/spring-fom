package com.fom.modules.importer.demo.st;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

import com.fom.context.ZipImporterConfig;
import com.fom.util.XmlUtil;


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
	protected boolean isValid(Element element) throws Exception {
		if(StringUtils.isBlank(userIndex)){ 
			LOG.warn("缺少配置：" + name + ".user.index");
			return false;
		}
		if(StringUtils.isBlank(userType)){ 
			LOG.warn("缺少配置：" + name + ".user.type");
			return false;
		}
		if(StringUtils.isBlank(userJson)){ 
			LOG.warn("缺少配置：" + name + ".user.json");
			return false;
		}
		userJsonFile = locationResource(userJson);
		
		if(StringUtils.isBlank(groupIndex)){ 
			LOG.warn("缺少配置：" + name + ".group.index");
			return false;
		}
		if(StringUtils.isBlank(groupType)){ 
			LOG.warn("缺少配置：" + name + ".group.type");
			return false;
		}
		if(StringUtils.isBlank(groupJson)){ 
			LOG.warn("缺少配置：" + name + ".group.json");
			return false;
		}
		groupJsonFile = locationResource(groupJson);
		
		if(StringUtils.isBlank(gzhIndex)){ 
			LOG.warn("缺少配置：" + name + ".gzh.index");
			return false;
		}
		if(StringUtils.isBlank(gzhType)){ 
			LOG.warn("缺少配置：" + name + ".gzh.type");
			return false;
		}
		if(StringUtils.isBlank(gzhJson)){ 
			LOG.warn("缺少配置：" + name + ".gzh.json");
			return false;
		}
		gzhJsonFile = locationResource(gzhJson);
		
		if(StringUtils.isBlank(zkAddress)){ 
			LOG.warn("缺少配置：" + name + ".zkAddress");
			return false;
		}
		return true;
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

		return userIndex.equals(config.getUserIndex()) && userType.equals(config.getUserType()) && userJson.equals(config.getUserJson())
				&& groupIndex.equals(config.getGroupIndex()) && groupType.equals(config.getGroupType()) && groupJson.equals(config.getGroupJson())
				&& gzhIndex.equals(config.getGzhIndex()) && gzhType.equals(config.getGzhType()) && gzhJson.equals(config.getGzhJson())
				&& zkAddress.equals(config.getZkAddress());
	}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\n" + name + ".user.index=" + userIndex);
		builder.append("\n" + name + ".user.type=" + userType);
		builder.append("\n" + name + ".user.json=" + userJson);
		builder.append("\n" + name + ".group.index=" + groupIndex);
		builder.append("\n" + name + ".group.type=" + groupType);
		builder.append("\n" + name + ".group.json=" + groupJson);
		builder.append("\n" + name + ".gzh.index=" + gzhIndex);
		builder.append("\n" + name + ".gzh.type=" + gzhType);
		builder.append("\n" + name + ".gzh.json=" + gzhJson);
		builder.append("\n" + name + ".zkAddress=" + zkAddress);
		return builder.toString();
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
