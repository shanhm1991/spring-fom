package com.fom.context;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fom.util.XmlUtil;

/**
 * <src.path>
 * <src.pattern>
 * <src.match.fail.del>
 * <scanner.cron>
 * <scanner>
 * <executor>
 * <executor.min>
 * <executor.max>
 * <executor.aliveTime.seconds>
 * <executor.overTime.seconds>
 * <executor.overTime.cancle>
 * <importer.batch>
 * <importer.zip.subPattern>
 * 
 * @author X4584
 * @date 2018年12月12日
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
	boolean valid() throws Exception {
		if(!super.valid()){
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
	
	public final boolean matchZipFile(String fileName){
		if(subPattern == null){
			return true;
		}
		return subPattern.matcher(fileName).find();
	}

}
