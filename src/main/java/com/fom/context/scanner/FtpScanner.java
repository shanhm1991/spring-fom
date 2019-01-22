package com.fom.context.scanner;

import java.util.List;

import com.fom.context.config.IFtpConfig;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class FtpScanner<E extends IFtpConfig> extends Scanner<E>{

	protected FtpScanner(String name, E config) {
		super(name, config);
	}

	@Override
	public List<String> scan(E config) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> filter(E config) {
		// TODO Auto-generated method stub
		return null;
	}

}
