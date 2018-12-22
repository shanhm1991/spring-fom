package com.fom.modules.importer;

import org.dom4j.Element;

import com.fom.context.ImporterConfig;

/**
 * 继承自父类的配置项，另外可以在<extended>节点中自定义配置项
 * 
 * @author X4584
 * @date 2018年12月21日
 *
 */
public class DemoConfig extends ImporterConfig {
	
	//自定义配置项,对应于节点<extended>

	protected DemoConfig(String name) {
		super(name);
	}
	
	/**
	 * 继承自Config，自定义加载<extended>中的配置项
	 */
	@Override
	protected void load(Element extendedElement) throws Exception {
		super.load(extendedElement);
		//...
	}
	
	/**
	 * 继承自Config，自定义校验<extended>中的配置项
	 */
	@Override
	protected boolean valid(Element extendedElement) throws Exception {
		return super.valid(extendedElement);
		//...
	}
	
	/**
	 * 需要继承父类复写，在打日志的时候以及页面展示的时候即调用的toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		//builder.append...
		return builder.toString();
	}
	
	/**
	 * 需要继承父类复写，再修改配置时判断config配置项有没有变化时即调用的equals(Object o)
	 */
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof DemoConfig)){
			return false;
		}
		if(o == this){
			return true;
		}

		DemoConfig config = (DemoConfig)o;
		boolean equal = super.equals(config);
		if(!equal){
			return false;
		}
		//equal=...
		return equal;
	}

}
