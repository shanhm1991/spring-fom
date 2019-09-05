package org.eto.fom.util.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.eto.fom.util.file.reader.ExcelEventReader;
import org.eto.fom.util.file.reader.ExcelReader;
import org.eto.fom.util.file.reader.ExcelRow;
import org.eto.fom.util.file.reader.ExcelSheetFilter;
import org.xml.sax.SAXException;

public class ExcelTest {

	public static void main(String[] args) throws Exception {
		test3();
	}
	
	public static void test3() throws Exception{
		ExcelEventReader reader = new ExcelEventReader("D:/10.xls");
		reader.setSheetFilter(new ExcelSheetFilter(){
			@Override
			public boolean filter(int sheetIndex, String sheetName) {
				return true;
			}

			@Override 
			public void resetSheetListForRead(List<String> nameList) {
				nameList.remove(0);
			}
		});
		
		ExcelRow row = null;
		while((row = reader.readRow()) != null){
			System.out.println(row);
		}
		reader.close();
	}

	public static void test2() throws Exception{
		ExcelEventReader reader = new ExcelEventReader("D:/1.xlsx");
		reader.setSheetFilter(new ExcelSheetFilter(){
			@Override
			public boolean filter(int sheetIndex, String sheetName) {
				return true;
			}

			@Override
			public void resetSheetListForRead(List<String> nameList) {
				nameList.remove(1);
			}
		});

		ExcelRow row = null;
		while((row = reader.readRow()) != null){
			System.out.println(row);
		}
		reader.close();
	}

	public static void test1() throws Exception{
		ExcelReader reader = new ExcelReader("D:/1.xlsx");
		reader.setSheetFilter(new ExcelSheetFilter(){
			@Override
			public boolean filter(int sheetIndex, String sheetName) {
				return true;
			}

			@Override
			public void resetSheetListForRead(List<String> nameList) {
				nameList.remove(0);
			}
		});

		ExcelRow row = null;
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

	public static void writeXls() throws IOException{ 
		HSSFWorkbook wb = new HSSFWorkbook();

		HSSFSheet sheet = wb.createSheet("s1");
		HSSFRow row = sheet.createRow(0);
		HSSFCellStyle style = wb.createCellStyle();    

		HSSFCell cell = row.createCell(0);         
		cell.setCellValue("c0");                  
		cell.setCellStyle(style);                  

		cell = row.createCell(1);                   
		cell.setCellValue("c1");
		cell.setCellStyle(style);

		cell = row.createCell(2);                   
		cell.setCellValue("c2");
		cell.setCellStyle(style);

		cell = row.createCell(3);                   
		cell.setCellValue("c3");
		cell.setCellStyle(style);

		List<ErrorCondition> list = getErrorCondition();
		for (int i = 0; i < list.size(); i++) {
			ErrorCondition errorCondition = list.get(i);
			//创建行
			row = sheet.createRow(i+1);
			//创建单元格并且添加数据
			row.createCell(0).setCellValue(errorCondition.getName());
			row.createCell(1).setCellValue(errorCondition.getIdCard());
			row.createCell(2).setCellValue(errorCondition.getStatus());
			row.createCell(3).setCellValue(errorCondition.getMessage());
		}


		HSSFSheet sheet2 = wb.createSheet("s2");
		HSSFRow row2 = sheet2.createRow(0);

		cell = row2.createCell(0);         
		cell.setCellValue("c0");                  
		cell.setCellStyle(style);                  

		cell = row2.createCell(1);                   
		cell.setCellValue("c1");
		cell.setCellStyle(style);

		cell = row2.createCell(2);                   
		cell.setCellValue("c2");
		cell.setCellStyle(style);

		cell = row2.createCell(3);                   
		cell.setCellValue("c3");
		cell.setCellStyle(style);
		
		for (int i = 0; i < list.size(); i++) {
			ErrorCondition errorCondition = list.get(i);
			//创建行
			row2 = sheet2.createRow(i+1);
			//创建单元格并且添加数据
			row2.createCell(0).setCellValue(errorCondition.getName());
			row2.createCell(1).setCellValue(errorCondition.getIdCard());
			row2.createCell(2).setCellValue(errorCondition.getStatus());
			row2.createCell(3).setCellValue(errorCondition.getMessage());
		}

		try {
			FileOutputStream fout = new FileOutputStream("D:\\10.xls");
			wb.write(fout);
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		wb.close();
		System.out.println("Excel文件生成成功...");

	}

	public static List<ErrorCondition> getErrorCondition(){
		List<ErrorCondition> list = new ArrayList<ErrorCondition>();
		ErrorCondition r1 = new ErrorCondition("张三", "4306821989021611", "L", "长度错误");
		ErrorCondition r2 = new ErrorCondition("李四", "430682198902191112","X", "校验错误");
		ErrorCondition r3 = new ErrorCondition("王五", "", "N", "身份证信息为空");
		list.add(r1);
		list.add(r2);
		list.add(r3);
		return list;
	}


	static class ErrorCondition {
		private String name; // 姓名
		private String idCard; // 身份证
		private String status; // 错误状态
		private String message; // 错误信息

		ErrorCondition(String name,String idCard,String status,String message){
			this.name = name;
			this.idCard = idCard;
			this.status = status;
			this.message = message;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getIdCard() {
			return idCard;
		}

		public void setIdCard(String idCard) {
			this.idCard = idCard;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}
}
