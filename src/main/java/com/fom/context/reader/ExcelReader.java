package com.fom.context.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fom.util.IoUtil;

/**
 * 
 * Excel适配，读取excel文件
 * 
 * @author shanhm
 *
 */
public class ExcelReader implements Reader {
	
	public static final String TYPE_XSL = "xsl";
	
	public static final String TYPE_XSLX = "xslx";
	
	private Workbook workbook = null;

	private int sheetCount;

	private Sheet sheet; 

	private int sheetIndex = 0;

	private String sheetName;

	private int rowIndex = 0;

	private int rowCount;

	/**
	 * 
	 * @param sourceUri sourceUri
	 * @param type xls/xlsx
	 * @throws IOException IOException
	 */
	public ExcelReader(String sourceUri, String type) throws IOException { 
		this(new FileInputStream(sourceUri), type);
	}

	/**
	 * 
	 * @param file file 
	 * @param type xls/xlsx
	 * @throws IOException IOException
	 */
	public ExcelReader(File file, String type) throws IOException { 
		this(new FileInputStream(file), type);
	}

	/**
	 *  
	 * @param inputStream inputStream 
	 * @param type xls/xlsx
	 * @throws IOException IOException
	 */
	public ExcelReader(InputStream inputStream, String type) throws IOException {
		if(TYPE_XSL.equals(type)){
			workbook = new HSSFWorkbook(inputStream);
		}else if(TYPE_XSLX.equals(type)){
			workbook = new XSSFWorkbook(inputStream);
		}else{
			throw new UnsupportedOperationException("Excel type can only be xls or xlsx.");
		}
		sheetCount = workbook.getNumberOfSheets();
		sheet = workbook.getSheetAt(sheetIndex);
		sheetName = sheet.getSheetName();
		rowCount = sheet.getPhysicalNumberOfRows();
	}

	@Override
	public RowData readRow() {
		while(true){
			if(!shouldSheetProcess(sheetIndex, sheetName)){
				sheetIndex++;
				if(sheetIndex >= sheetCount){
					return null;
				}
				initSheet();
			}else{
				if(rowIndex >= rowCount){
					if(sheetIndex >= sheetCount - 1){
						return null;
					}else{
						sheetIndex++;
						initSheet();
						continue;
					}
				}else{
					Row row = sheet.getRow(rowIndex);
					rowIndex++;
					
					int cellCount = row.getPhysicalNumberOfCells();
					List<String> list = new ArrayList<>();
					for(int i = 0;i < cellCount;i++){
						Cell cell = row.getCell(i);
						if(cell == null){
							list.add(null);
						}else{
							list.add(row.getCell(i).getStringCellValue());
						}
					}
					
					RowData rowData = new RowData(rowIndex - 1, list);
					rowData.setSheetIndex(sheetIndex); 
					rowData.setSheetName(sheetName); 
					return rowData;
				}
			}
		}
	}
	
	private void initSheet(){
		sheet = workbook.getSheetAt(sheetIndex);
		sheetName = sheet.getSheetName();
		rowIndex = 0;
		rowCount = sheet.getPhysicalNumberOfRows();
	}
	
	@Override
	public void close() throws IOException {
		IoUtil.close(workbook); 
	}

	protected boolean shouldSheetProcess(int sheetIndex, String sheetName) {
		return true;
	}
}
