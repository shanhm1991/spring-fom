package org.eto.fom.util.test;

import org.eto.fom.util.file.reader.ExcelEventReader;
import org.eto.fom.util.file.reader.ReaderRow;

public class Test {

	public static void main(String[] args) throws Exception {
		//ExcelReader reader = new ExcelReader("D:/1.xlsx");
		
		ExcelEventReader reader = new ExcelEventReader("D:/1.xlsx");
		
		ReaderRow row = null;
		while((row = reader.readRow()) != null){
			System.out.println(row);
		}
		reader.close();
	}
}
