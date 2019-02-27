package com.fom.context.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.fom.util.IoUtil;

/**
 * BufferedReader适配，读取普通文本文件
 * 
 * @author shanhm
 *
 */
public class TextReader implements Reader {
	
	private BufferedReader reader;
	
	private String regex;
	
	/**
	 * @param regex
	 * @param sourceUri sourceUri
	 * @throws Exception Exception
	 */
	public TextReader(String sourceUri, String regex) throws Exception {
		this(new FileInputStream(new File(sourceUri)), regex);
	}
	
	/**
	 * @param regex
	 * @param file file
	 * @throws Exception Exception
	 */
	public TextReader(File file, String regex) throws Exception {
		this(new FileInputStream(file), regex);
	}
	
	/**
	 * @param regex
	 * @param inputStream inputStream
	 * @throws Exception Exception
	 */
	public TextReader(InputStream inputStream, String regex) throws Exception {
		this.regex = regex;
		reader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
	}

	@Override
	public List<String> readLine() throws Exception {
		String line = reader.readLine();
		if(line == null){
			return null;
		}
		if(regex == null){
			return Arrays.asList(line);
		}
		return Arrays.asList(line.split(regex));
	}

	@Override
	public void close() throws IOException {
		IoUtil.close(reader);
	}

}
