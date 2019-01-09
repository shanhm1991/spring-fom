package com.fom.context.config;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class UploaderConfig extends Config {

	protected UploaderConfig(String name) {
		super(name);
	}

	@Override
	void load() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public final String getType() {
		return TYPE_UPLOADER;
	}

	@Override
	public final String getTypeName() {
		return NAME_UPLOADER;
	}

}
