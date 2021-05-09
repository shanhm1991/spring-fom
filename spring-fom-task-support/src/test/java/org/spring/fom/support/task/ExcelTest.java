package org.spring.fom.support.task;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spring.fom.support.task.reader.ExcelEventReader;
import org.spring.fom.support.task.reader.ExcelReader;
import org.spring.fom.support.task.reader.ExcelRow;
import org.spring.fom.support.task.reader.IExcelReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ExcelTest {
	
	private static Logger logger = LoggerFactory.getLogger(ExcelTest.class);
	
	@Test
	public void testExcelReader() throws Exception{ 
		IExcelReader reader = new ExcelReader("ExcelTest.xlsx");
		ExcelRow row = null;
		while((row = reader.readRow()) != null){
			logger.info(row.toString());
		}
		reader.close();
	}

	@Test
	public void testExcelEventReader() throws Exception{ 
		IExcelReader reader = new ExcelEventReader("ExcelTest.xlsx");
		ExcelRow row = null;
		while((row = reader.readRow()) != null){
			logger.info(row.toString());
		}
		reader.close();
	}
	
	@Test
	public void testSheetReader() throws Exception{ 
		Resource resource = new ClassPathResource("SheetTest.xlsx");
		File excel = resource.getFile();
		
		SheetTask sheetTask = new SheetTask(excel);
		sheetTask.setExcelRule("sheetRule.xml");
		sheetTask.call();
	}
}
