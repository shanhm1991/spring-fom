package org.spring.fom.support.task;

import org.junit.Test;
import org.spring.fom.support.task.reader.ExcelEventReader;
import org.spring.fom.support.task.reader.ExcelReader;
import org.spring.fom.support.task.reader.ExcelRow;
import org.spring.fom.support.task.reader.IExcelReader;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ExcelTest {
	
	@Test
	public void testExcelReader() throws Exception{ 
		IExcelReader reader = new ExcelReader("ExcelTest.xlsx");
		ExcelRow row = null;
		while((row = reader.readRow()) != null){
		    System.out.println(row);
		}
		reader.close();
	}

	@Test
	public void testExcelEventReader() throws Exception{ 
		IExcelReader reader = new ExcelEventReader("ExcelTest.xlsx");
		ExcelRow row = null;
		while((row = reader.readRow()) != null){
		    System.out.println(row);
		}
		reader.close();
	}
}
