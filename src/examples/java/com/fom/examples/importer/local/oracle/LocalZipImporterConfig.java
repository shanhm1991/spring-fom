package com.fom.examples.importer.local.oracle;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

import com.fom.context.Config;
import com.fom.context.executor.config.ILocalZipImporterConfig;
import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalZipImporterConfig extends Config implements ILocalZipImporterConfig {
	
	private int batch;
	
	private Pattern pattern;

	protected LocalZipImporterConfig(String name) {
		super(name);
	}

	@Override
	public String getType() {
		return TYPE_IMPORTER;
	}

	@Override
	public String getTypeName() {
		return TYPENAME_IMPORTER;
	}
	
	@Override
	public int getBatch() {
		return batch;
	}

	@Override
	public boolean matchSubFile(String fileName) {
		if(pattern == null){
			return true;
		}
		return pattern.matcher(fileName).find();
	}
	
	@Override
	protected void load(Element e) throws Exception {
		batch = XmlUtil.getInt(e, "importer.batch", 5000, 1, 50000);
		String reg = XmlUtil.getString(e, "zip.subPattern", "");
		if(!StringUtils.isBlank(reg)){
			pattern = Pattern.compile(reg);
		}
	}
}
