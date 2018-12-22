package com.fom.test;

import java.util.regex.Pattern;

public class PatternTest {
	
	public static void main(String[] args) {
		
		Pattern pattern = Pattern.compile(".\\.txt$");
		
		System.out.println(pattern.matcher("新建文本文档.txt").find()); 
	}

}
