package com.fom.context.scanner;

import java.util.List;

import com.fom.context.Scanner;

/**
 * 
 * @author shanhm
 *
 * @param <E>
 */
public class FtpScanner<E extends FtpConfig> extends Scanner<E>{

	protected FtpScanner(String name) {
		super(name);
	}

	@Override
	public List<String> scan(String srcUri, E config) {
		// TODO Auto-generated method stub
		return null;
	}

}
