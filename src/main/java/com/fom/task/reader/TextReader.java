package com.fom.task.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.fom.util.IoUtil;

/**
 * BufferedReader适配，读取普通文本文件
 * 
 * @author shanhm
 *
 */
public class TextReader implements Reader {
	
	private BufferedReader reader;
	
	private int rowIndex;
	
	private String regex;
	
	/**
	 * @param regex regex
	 * @param sourceUri sourceUri
	 * @throws Exception Exception
	 */
	public TextReader(String sourceUri, String regex) throws Exception {
		this(new FileInputStream(new File(sourceUri)), regex);
	}
	
	/**
	 * @param regex regex
	 * @param file file
	 * @throws Exception Exception
	 */
	public TextReader(File file, String regex) throws Exception {
		this(new FileInputStream(file), regex);
	}
	
	/**
	 * @param regex regex
	 * @param inputStream inputStream
	 * @throws Exception Exception
	 */
	public TextReader(InputStream inputStream, String regex) throws Exception {
		this.regex = regex;
		reader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
	}

	@Override
	public TextRow readRow() throws Exception {
		String line = reader.readLine();
		rowIndex++;
		if(line == null){
			return null;
		}
		
		boolean isEmpty = StringUtils.isBlank(line);
		
		if(regex == null){
			TextRow row = new TextRow(rowIndex - 1, Arrays.asList(line));
			row.setEmpty(isEmpty);
			return row;
		}
		TextRow row = new TextRow(rowIndex - 1, Arrays.asList(line.split(regex)));
		row.setEmpty(isEmpty);
		return row;
	}

	@Override
	public void close() throws IOException {
		IoUtil.close(reader);
	}

}
