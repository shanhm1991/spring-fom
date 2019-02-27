package com.fom.context.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.fom.util.IoUtil;

/**
 * 
 * Excel适配，读取excel文件
 * 
 * @author shanhm
 *
 */
public class ExcelReader implements Reader {

	private HSSFWorkbook workbook = null;

	private int sheetCount;

	private HSSFSheet sheet;

	private int sheetIndex = 0;

	private String sheetName;

	private int rowIndex = 0;

	private int rowCount;

	/**
	 * 
	 * @param sourceUri sourceUri
	 * @throws IOException IOException
	 */
	public ExcelReader(String sourceUri) throws IOException { 
		this(new FileInputStream(sourceUri));
	}

	/**
	 * 
	 * @param file file 
	 * @throws IOException IOException
	 */
	public ExcelReader(File file) throws IOException { 
		this(new FileInputStream(file));
	}

	/**
	 * 
	 * @param inputStream inputStream 
	 * @throws IOException IOException
	 */
	public ExcelReader(InputStream inputStream) throws IOException {
		workbook = new HSSFWorkbook(inputStream);
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
				sheet = workbook.getSheetAt(sheetIndex);
				sheetName = sheet.getSheetName();
				rowIndex = 0;
				rowCount = sheet.getPhysicalNumberOfRows();
			}else{
				if(rowIndex >= rowCount){
					if(sheetIndex >= sheetCount - 1){
						return null;
					}else{
						continue;
					}
				}else{
					HSSFRow row = sheet.getRow(rowIndex);
					rowIndex++;
					
					int colCount = row.getPhysicalNumberOfCells();
					List<String> list = new ArrayList<>();
					for(int i = 0;i < colCount;i++){
						list.add(row.getCell(i).getStringCellValue());
					}
					
					RowData rowData = new RowData(rowIndex - 1, list);
					rowData.setSheetIndex(sheetIndex); 
					rowData.setSheetName(sheetName); 
					return rowData;
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		IoUtil.close(workbook); 
	}

	protected boolean shouldSheetProcess(int sheetIndex, String sheetName) {
		return true;
	}
}
