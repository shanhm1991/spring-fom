package com.fom.examples.importer.local.mysql;

import org.dom4j.Element;

import com.fom.context.config.Config;
import com.fom.context.executor.IImporterConfig;
import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalImporterConfig extends Config implements IImporterConfig {
	
	private int batch;

	protected LocalImporterConfig(String name) {
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
	protected void load(Element e) throws Exception {
		batch = XmlUtil.getInt(e, "importer.batch", 5000, 1, 50000);
	}
}
