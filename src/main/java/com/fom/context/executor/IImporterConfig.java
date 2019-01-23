package com.fom.context.executor;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public interface IImporterConfig {
	
	int getBatch();

//	int batch;
//
//	File progressDir = null;

//	@Override
//	void load() throws Exception {
//		super.load();
//		batch = XmlUtil.getInt(element, "importer.batch", 5000, 1, 50000);
//	}
//	
//	@Override
//	boolean isValid() throws Exception {
//		if(!super.isValid()){
//			return false;
//		}
//		progressDir = new File(System.getProperty("import.progress") + File.separator + name);
//		if(!progressDir.exists() && !progressDir.mkdirs()){
//			LOG.error("创建处理目录失败:" + progressDir.getPath()); 
//			return false;
//		}
//		return true;
//	}
	
//	@Override
//	public String toString() {
//		StringBuilder builder = new StringBuilder(super.toString());
//		builder.append("\nimporter.batch=" + batch);
//		return builder.toString();
//	}
//	
//	@Override
//	public boolean equals(Object o){
//		if(!(o instanceof ImporterConfig)){
//			return false;
//		}
//		if(o == this){
//			return true;
//		}
//		
//		ImporterConfig c = (ImporterConfig)o; 
//		if(!super.equals(c)){
//			return false;
//		}
//		
//		return batch == c.batch;
//	}
//
//	@Override
//	public final String getType() {
//		return TYPE_IMPORTER;
//	}
//
//	@Override
//	public final String getTypeName() {
//		return TYPENAME_IMPORTER;
//	}
//
//	public final int getBatch() {
//		return batch;
//	}
	
}
