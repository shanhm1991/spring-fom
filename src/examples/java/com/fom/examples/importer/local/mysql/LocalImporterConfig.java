package com.fom.examples.importer.local.mysql;

import org.dom4j.Element;

import com.fom.context.config.ImporterConfig;
import com.fom.util.XmlUtil;

/**
 * 继承自父类配置项，另外可以在<extended>节点中加载自定义配置
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalImporterConfig extends ImporterConfig {
	
	private String myconf;
	
	//...
	
	protected LocalImporterConfig(String name) {
		super(name);
	}
	
	/**
	 * 继承自Config，自定义加载<extended>中的配置项
	 */
	@Override
	protected void load(Element extendedElement) throws Exception {
		myconf = XmlUtil.getString(extendedElement, "demo.conf", ""); 
		//...
	}
	
	/**
	 * 继承自Config，自定义校验<extended>中的配置项
	 */
	@Override
	protected boolean valid() throws Exception {
		//myconf is ok
		return true;
	}
	
	/**
	 * 需要继承父类复写，在打日志的时候以及页面展示的时候即调用的toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\ndemo.conf=" + myconf);
		//...
		return builder.toString();
	}
	
	/**
	 * 需要继承父类复写，再修改配置时判断config配置项有没有变化时即调用的equals(Object o)
	 */
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof LocalImporterConfig)){
			return false;
		}
		if(o == this){
			return true;
		}

		LocalImporterConfig config = (LocalImporterConfig)o;
		boolean equal = super.equals(config);
		if(!equal){
			return false;
		}
		
		//...
		return myconf.equals(config.myconf); 
	}

}
