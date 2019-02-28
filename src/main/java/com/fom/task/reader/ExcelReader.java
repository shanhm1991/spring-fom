package com.fom.task.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

	public static final String TYPE_XLS = "xls";

	public static final String TYPE_XLSX = "xlsx";

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
	 * @throws IOException IOException
	 */
	public ExcelReader(String sourceUri) throws IOException {
		int index = sourceUri.lastIndexOf('.');
		if(index == -1){
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx.");
		}
		String type = sourceUri.substring(index + 1);
		init(new FileInputStream(sourceUri), type);
	}

	/**
	 * 
	 * @param file file 
	 * @throws IOException IOException
	 */
	public ExcelReader(File file) throws IOException {
		String name = file.getName();
		int index = name.lastIndexOf('.');
		if(index == -1){
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx");
		}
		String type = name.substring(index + 1);
		init(new FileInputStream(file), type);
	}

	/**
	 *  
	 * @param inputStream inputStream 
	 * @param type xls/xlsx
	 * @throws IOException IOException
	 */
	public ExcelReader(InputStream inputStream, String type) throws IOException {
		init(inputStream, type);
	}

	private void init(InputStream inputStream, String type) throws IOException{ 
		if(TYPE_XLS.equals(type)){
			workbook = new HSSFWorkbook(inputStream);
		}else if(TYPE_XLSX.equals(type)){
			workbook = new XSSFWorkbook(inputStream);
		}else{
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx");
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
						list.add(getCellValue(row.getCell(i)));
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

	private String getCellValue(Cell cell) {
		if (cell == null) {
			return "null";
		}

		switch (cell.getCellType()) {
		case NUMERIC:
			return double2String(cell.getNumericCellValue());
		case STRING:
			return cell.getStringCellValue();
		case BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());
		case FORMULA:
			try {
				return double2String(cell.getNumericCellValue());
			} catch (IllegalStateException e) {
				try {
					return cell.getRichStringCellValue().toString();
				} catch (IllegalStateException e2) {
					return "error";
				}
			} catch (Exception e) {
				return "error";
			}
		case BLANK:
			return "";
		case ERROR:
			return "error";
		default:
			return "";
		}
	}

	private static String double2String(Double d) {
		String doubleStr = d.toString();
		boolean b = doubleStr.contains("E");
		int indexOfPoint = doubleStr.indexOf('.');
		if (b) {
			int indexOfE = doubleStr.indexOf('E');
			// 小数部分
			BigInteger xs = new BigInteger(doubleStr.substring(indexOfPoint
					+ BigInteger.ONE.intValue(), indexOfE));
			// 指数
			int pow = Integer.parseInt(doubleStr.substring(indexOfE + BigInteger.ONE.intValue()));
			int xsLen = xs.toByteArray().length;
			int scale = xsLen - pow > 0 ? xsLen - pow : 0;
			doubleStr = String.format("%." + scale + "f", d);
		} else {
			java.util.regex.Pattern p = Pattern.compile(".0$");
			java.util.regex.Matcher m = p.matcher(doubleStr);
			if (m.find()) {
				doubleStr = doubleStr.replace(".0", "");
			}
		}
		return doubleStr;
	}

	@Override
	public void close() throws IOException {
		IoUtil.close(workbook); 
	}

	/**
	 * 
	 * @param sheetIndex sheetIndex
	 * @param sheetName sheetName
	 * @return boolean
	 */
	protected boolean shouldSheetProcess(int sheetIndex, String sheetName) {
		return true;
	}
}
