package org.eto.fom.util.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.eto.fom.util.file.reader.ExcelReader;
import org.eto.fom.util.file.reader.IRow;
import org.xml.sax.SAXException;

public class ExcelTest {

	public static void main(String[] args) throws Exception {
		ExcelReader reader = new ExcelReader("D:/1.xlsx");
		
		//ExcelEventReader reader = new ExcelEventReader("D:/1.xlsx");
		
		List<Integer> list = new ArrayList<>();
		list.add(2);
		list.add(1);
		reader.setSheetListForReadByIndex(list);

		IRow row = null;
		while((row = reader.readRow()) != null){
			System.out.println(row);
		}
		reader.close();

	}

	public static void printStruct() throws IOException, OpenXML4JException, SAXException{
		OPCPackage pkg = OPCPackage.open("D:/1.xlsx");
		XSSFReader reader = new XSSFReader(pkg);
		InputStream bookStream = reader.getWorkbookData();
		byte[] buf = new byte[1024];
		int len;
		while ((len = bookStream.read(buf)) != -1) {
			System.out.write(buf, 0, len);
		}

		Iterator<InputStream> it = reader.getSheetsData();
		while(it.hasNext()){
			InputStream in = it.next();
			while ((len = in.read(buf)) != -1) {
				System.out.write(buf, 0, len);
			}
		}
	}
}
