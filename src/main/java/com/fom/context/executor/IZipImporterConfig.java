package com.fom.context.executor;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public interface IZipImporterConfig extends IImporterConfig {

//	protected ZipImporterConfig(String name) {
//		super(name);
//	}
//
//	String subReg;
//
//	Pattern subPattern;
//	
//	@Override
//	void load() throws Exception {
//		super.load();
//		subReg = XmlUtil.getString(element, "importer.zip.subPattern", "");
//		if(!StringUtils.isBlank(subReg)){
//			subPattern = Pattern.compile(subReg);
//		}
//	}
//	
//	@Override
//	boolean isValid() throws Exception {
//		if(!super.isValid()){
//			return false;
//		}
//		if(!StringUtils.isBlank(subReg)){
//			subPattern = Pattern.compile(subReg);
//		}
//		return true;
//	}
//	
//	@Override
//	public String toString() {
//		StringBuilder builder = new StringBuilder(super.toString());
//		builder.append("\nimporter.zip.subPattern=" + subReg);
//		return builder.toString();
//	}
//	
//	@Override
//	public boolean equals(Object o){
//		if(!(o instanceof ZipImporterConfig)){
//			return false;
//		}
//		if(o == this){
//			return true;
//		}
//		
//		ZipImporterConfig c = (ZipImporterConfig)o; 
//		if(!super.equals(c)){
//			return false;
//		}
//		
//		return subReg.equals(c.subReg);
//	}
//	
//	public final boolean matchZipContent(String fileName){
//		if(subPattern == null){
//			return true;
//		}
//		return subPattern.matcher(fileName).find();
//	}

}
