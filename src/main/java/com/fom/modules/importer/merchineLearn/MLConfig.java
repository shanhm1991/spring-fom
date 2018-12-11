package com.fom.modules.importer.merchineLearn;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.fom.context.SDZipImporterConfig;
import com.fom.util.XmlUtil;
import com.fom.util.log.LoggerFactory;

public class MLConfig extends SDZipImporterConfig {
	
	private static final Logger LOG = LoggerFactory.getLogger("config");
	
	private String esIndex;

	private String esType;
	
	private String esJson;
	
	private String filed;
	
	private File esJsonFile;

	private List<String> filedList;

	private Map<String,String> patternMap;

	protected MLConfig(String name) {
		super(name);
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

	public String getFiled() {
		return filed;
	}

	public List<String> getFiledList() {
		return filedList;
	}

	public Map<String, String> getPatternMap() {
		return patternMap;
	}

	@Override
	protected void load(Element element) throws Exception {
		if(element == null){
			return;
		}
		esIndex = XmlUtil.getString(element, "es.index", "");
		esType = XmlUtil.getString(element, "es.type", "");
		esJson = XmlUtil.getString(element, "es.json", "");
		filed = XmlUtil.getString(element, "filed", "");
	}
	
	@Override
	protected boolean isValid(Element element) throws IOException {
		if(StringUtils.isBlank(esIndex)){ 
			LOG.warn("缺少配置:es.index");
			return false;
		}
		if(StringUtils.isBlank(esType)){ 
			LOG.warn("缺少配置:es.type");
			return false;
		}
		if(StringUtils.isBlank(esJson)){ 
			LOG.warn("缺少配置:es.json");
			return false;
		}
		esJsonFile = locationResource(esJson);
		
		if(StringUtils.isBlank(filed)){ 
			LOG.warn("缺少配置:filed");
			return false;
		}
		filedList = Arrays.asList(filed.replaceAll("\\s*", "").split(","));//文件解析字段
		if(!filedList.contains("DOCID") || !filedList.contains("HIT_TIMES") 
				|| !filedList.contains("FTIME") || !filedList.contains("LTIME")){
			throw new RuntimeException("filed缺少必要字段DOCID，HIT_TIMES，FTIME，LTIME");
		}
		
		patternMap = new HashMap<String,String>();
		for(String filed : filedList){
			String pattern = XmlUtil.getString(element, "pattern." + filed.trim(), "");
			if(StringUtils.isBlank(pattern)){
				LOG.warn("缺少配置,字段" + filed + "没有配置获取方式");
				return false;
			}
			patternMap.put(filed.trim(), pattern.trim());
		}
		return true;
	}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nes.index=" + esIndex);
		builder.append("\nes.type=" + esType);
		builder.append("\nes.json=" + esJsonFile);
		builder.append("\nfiled=" + filedList);
		builder.append("\nfiled.pattern=" + patternMap);
		return builder.toString();
	}
}
