package com.fom.context.config;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fom.util.XmlUtil;

/**
 * importer.zip.subPattern 根据正则表达式匹配zip中的文件是否需要处理
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class ZipImporterConfig extends ImporterConfig {

	protected ZipImporterConfig(String name) {
		super(name);
	}

	String subReg;

	Pattern subPattern;
	
	@Override
	void load() throws Exception {
		super.load();
		subReg = XmlUtil.getString(element, "importer.zip.subPattern", "");
		if(!StringUtils.isBlank(subReg)){
			subPattern = Pattern.compile(subReg);
		}
	}
	
	@Override
	boolean isValid() throws Exception {
		if(!super.isValid()){
			return false;
		}
		if(!StringUtils.isBlank(subReg)){
			subPattern = Pattern.compile(subReg);
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nimporter.zip.subPattern=" + subReg);
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof ZipImporterConfig)){
			return false;
		}
		if(o == this){
			return true;
		}
		
		ZipImporterConfig c = (ZipImporterConfig)o; 
		if(!super.equals(c)){
			return false;
		}
		
		return subReg.equals(c.subReg);
	}
	
	public final boolean matchZipContent(String fileName){
		if(subPattern == null){
			return true;
		}
		return subPattern.matcher(fileName).find();
	}

}
