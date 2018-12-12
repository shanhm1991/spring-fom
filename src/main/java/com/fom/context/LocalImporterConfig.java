package com.fom.context;

import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm1991
 *
 */
public class LocalImporterConfig extends Config {
	
	int batch;
	
	protected LocalImporterConfig(String name) {
		super(name);
	}

	@Override
	void load() throws Exception {
		super.load();
		batch = XmlUtil.getInt(element, "importer.batch", 5000, 1, 50000);
	}
	
	@Override
	public final String getType() {
		return TYPE_IMPORTER;
	}

	@Override
	public final String getTypeName() {
		return NAME_IMPORTER;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nimporter.batch=" + batch);
		return builder.toString();
	}

	@Override
	public boolean equals(Object o){
		if(!(o instanceof LocalImporterConfig)){
			return false;
		}
		if(o == this){
			return true;
		}
//		Config c = (Config)o; 

		//		boolean equal = importer.equals(c.importer) && srcPathName.equals(c.srcPathName) && pool.equals(c.pool)  
		//				&& reg.equals(c.reg) && cycle == c.cycle && configClass.equals(c.configClass) && isHDFS == c.isHDFS; 
		//		if(equal){
		//			equal = srcType.equals(c.srcType) && delMatchFailFile ==  c.delMatchFailFile && zipReg.equals(c.zipReg)
		//					&& importerMax == c.importerMax && importerAliveTime == c.importerAliveTime && importerOverTime == c.importerOverTime
		//					&& cancelWhenOverTime == c.cancelWhenOverTime && importerBatch == c.importerBatch;
		//		}
		return false;
	}


}
