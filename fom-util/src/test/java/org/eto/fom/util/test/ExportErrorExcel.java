package org.eto.fom.util.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class ExportErrorExcel {
	public static void main(String[] args) {
		//第一步创建workbook
		HSSFWorkbook wb = new HSSFWorkbook();
		
		//第二步创建sheet
		HSSFSheet sheet = wb.createSheet("身份证错误信息");
		
		//第三步创建行row:添加表头0行
		HSSFRow row = sheet.createRow(0);
		HSSFCellStyle  style = wb.createCellStyle();    
		
		
		//第四步创建单元格
		HSSFCell cell = row.createCell(0);         //第一个单元格
		cell.setCellValue("姓名");                  //设定值
		cell.setCellStyle(style);                   //内容居中
		
		cell = row.createCell(1);                   //第二个单元格   
		cell.setCellValue("身份证");
		cell.setCellStyle(style);
		
		cell = row.createCell(2);                   //第三个单元格  
		cell.setCellValue("错误状态");
		cell.setCellStyle(style);
		
		cell = row.createCell(3);                   //第四个单元格  
		cell.setCellValue("错误信息");
		cell.setCellStyle(style);
		
		//第五步插入数据
		List<ErrorCondition> list = ExportErrorExcel.getErrorCondition();
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
		
		//第六步将生成excel文件保存到指定路径下
		try {
			FileOutputStream fout = new FileOutputStream("D:\\errorCondition.xls");
			wb.write(fout);
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
}
