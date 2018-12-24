package com.fom.test;

import java.util.regex.Pattern;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class PatternTest {
	
	public static void main(String[] args) {
		Pattern pattern = Pattern.compile(".\\.txt$");
		
		System.out.println(pattern.matcher("新建文本文档.txt").find()); 
		
		System.out.println("sdwd.xml.ddw".contains(".xml."));
	}

}
