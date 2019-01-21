package com.fom.strategy;

import java.io.File;
import java.io.InputStream;

/**
 * 
 * @author shanhm1991
 * @date 2019年1月21日
 *
 */
public class HdfsOperator implements Operator {

	@Override
	public InputStream open(String url) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void download(String url, File file) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean delete(String url) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

}
