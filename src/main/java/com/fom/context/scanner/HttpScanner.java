package com.fom.context.scanner;

import java.util.List;

import com.fom.context.config.IHttpConfig;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class HttpScanner<E extends IHttpConfig> extends Scanner<E>{

	public HttpScanner(String name, E config) {
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
