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

	private boolean withSheetInfo = false;

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

	/**
	 * 
	 * @param sourceUri sourceUri 
	 * @param withSheetInfo 读取字段时是否带上sheet信息，在第0列为sheetIndex，第1列为sheetName
	 * @throws IOException IOException
	 */
	public ExcelReader(String sourceUri, boolean withSheetInfo) throws IOException {
		this(new FileInputStream(sourceUri), withSheetInfo);
	}

	/**
	 *  
	 * @param file
	 * @param withSheetInfo 读取字段时是否带上sheet信息，在第0列为sheetIndex，第1列为sheetName
	 * @throws IOException IOException
	 */
	public ExcelReader(File file, boolean withSheetInfo) throws IOException {
		this(new FileInputStream(file), withSheetInfo);
	}

	/**
	 *  
	 * @param inputStream
	 * @param withSheetInfo 读取字段时是否带上sheet信息，在第0列为sheetIndex，第1列为sheetName
	 * @throws IOException IOException
	 */
	public ExcelReader(InputStream inputStream, boolean withSheetInfo) throws IOException {
		this.withSheetInfo = withSheetInfo;
		workbook = new HSSFWorkbook(inputStream);
		sheetCount = workbook.getNumberOfSheets();
		sheet = workbook.getSheetAt(sheetIndex);
		sheetName = sheet.getSheetName();
		rowCount = sheet.getPhysicalNumberOfRows();
	}

	@Override
	public ReadRow readLine() {
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
					HSSFRow rowData = sheet.getRow(rowIndex);
					rowIndex++;
					
					int colCount = rowData.getPhysicalNumberOfCells();
					List<String> list = null;
					if(withSheetInfo){
						list = new ArrayList<>(colCount + 2);
						list.add(String.valueOf(sheetIndex));
						list.add(sheetName);
					}else{
						list = new ArrayList<>();
					}
					
					for(int i = 0;i < colCount;i++){
						list.add(rowData.getCell(i).getStringCellValue());
					}
					
					ReadRow readRow = new ReadRow(rowIndex - 1, list);
					readRow.setSheetIndex(sheetIndex); 
					readRow.setSheetName(sheetName); 
					return readRow;
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
