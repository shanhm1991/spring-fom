package com.fom.examples.importer.local.oracle;

import org.dom4j.Element;

import com.fom.context.config.ZipImporterConfig;
import com.fom.util.XmlUtil;

/**
 * 继承自父类配置项，另外可以在<extended>节点中加载自定义配置
 * 
 * @author X4584
 * @date 2018年12月22日
 *
 */
/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalZipImporterConfig extends ZipImporterConfig {

	private String myconf;

	//...

	protected LocalZipImporterConfig(String name) {
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
		if(!(o instanceof LocalZipImporterConfig)){
			return false;
		}
		if(o == this){
			return true;
		}

		LocalZipImporterConfig config = (LocalZipImporterConfig)o;
		boolean equal = super.equals(config);
		if(!equal){
			return false;
		}

		//...
		return myconf.equals(config.myconf); 
	}

}
