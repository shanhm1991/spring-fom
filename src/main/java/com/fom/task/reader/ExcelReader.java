package com.fom.task.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
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

	private static final Logger LOG = Logger.getLogger(ExcelReader.class);

	public static final String TYPE_XLS = "xls";

	public static final String TYPE_XLSX = "xlsx";
	
	private InputStream inputStream;

	private String type;

	private Workbook workbook = null;

	private Map<String, Sheet> sheetMap = new HashMap<>();
	
	private List<String> sheetRangeList = new ArrayList<>();
	
	private int rangeIndex = 0;
	
	private Sheet sheet; 

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
		this.type = sourceUri.substring(index + 1);
		this.inputStream = new FileInputStream(sourceUri);
		init();
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
		this.type = name.substring(index + 1);
		this.inputStream = new FileInputStream(file);
		init();
	}

	/**
	 *  
	 * @param inputStream inputStream 
	 * @param type xls/xlsx
	 * @throws IOException IOException
	 */
	public ExcelReader(InputStream inputStream, String type) throws IOException {
		this.type = type;
		this.inputStream = inputStream;
		init();
	}

	private void init() throws IOException{ 
		if(TYPE_XLS.equalsIgnoreCase(type)){
			workbook = new HSSFWorkbook(inputStream);
		}else if(TYPE_XLSX.equalsIgnoreCase(type)){
			workbook = new XSSFWorkbook(inputStream);
		}else{
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx");
		}
		int sheetCount = workbook.getNumberOfSheets();
		for(int i = 0;i < sheetCount;i++){
			Sheet shee = workbook.getSheetAt(i);
			sheetRangeList.add(shee.getSheetName());
			sheetMap.put(shee.getSheetName(), shee);
		}
		sheetRangeList = reRangeSheet(sheetRangeList);
		initSheet();
	}

	@Override
	public RowData readRow() {
		while(true){
			if(sheet == null){
				return null;
			}
			if(!shouldSheetProcess(rangeIndex, sheetName)){
				if(++rangeIndex >= sheetRangeList.size()){
					return null;
				}
				initSheet();
			}else{
				if(rowIndex >= rowCount){
					if(rangeIndex >= sheetRangeList.size() - 1){
						return null;
					}else{
						rangeIndex++;
						initSheet();
						continue;
					}
				}else{
					Row row = sheet.getRow(rowIndex);
					rowIndex++;
					
					//row not exist, don't know why
					if(row == null){
						RowData rowData = new RowData(rowIndex - 1, new ArrayList<String>(0));
						rowData.setSheetIndex(rangeIndex); 
						rowData.setSheetName(sheetName); 
						rowData.setEmpty(true); 
						rowData.setLastRow(rowIndex == rowCount); 
						return rowData;
					}

					int cellCount = row.getLastCellNum();
					List<String> list = new ArrayList<>(cellCount);
					
					boolean isEmpty = true;
					for(int i = 0;i < cellCount;i++){
						String value = getCellValue(row.getCell(i));
						if(isEmpty && !StringUtils.isBlank(value)){
							isEmpty = false;
						}
						list.add(value);
					}
					RowData rowData = new RowData(rowIndex - 1, list);
					rowData.setSheetIndex(rangeIndex); 
					rowData.setSheetName(sheetName); 
					rowData.setEmpty(isEmpty); 
					rowData.setLastRow(rowIndex == rowCount); 
					return rowData;
				}
			}
		}
	}

	private void initSheet(){
		rowIndex = 0;
		sheetName = sheetRangeList.get(rangeIndex); 
		while((sheet = sheetMap.get(sheetName)) == null){
			if(++rangeIndex >= sheetRangeList.size()){
				sheet = null;
				return;
			}
		}
		rowCount = sheet.getPhysicalNumberOfRows();
	}

	private String getCellValue(Cell cell) {
		if (cell == null) {
			return "";
		}

		switch (cell.getCellType()) {
		case NUMERIC:
			double value = cell.getNumericCellValue();
			if(DateUtil.isCellDateFormatted(cell)){
				Date date = DateUtil.getJavaDate(value);
				return String.valueOf(date.getTime());
			}else{
				return double2String(value);
			}
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
					LOG.error("Excel parse error: sheet=" + cell.getSheet().getSheetName() 
							+ ",row=" + cell.getRowIndex() + ",column=" + cell.getColumnIndex(), e2);
					return "error";
				}
			} catch (Exception e) {
				LOG.error("Excel parse error: sheet=" + cell.getSheet().getSheetName() 
						+ ",row=" + cell.getRowIndex() + ",column=" + cell.getColumnIndex(), e);
				return "";
			}
		case BLANK:
			return "";
		case ERROR:
			LOG.error("Excel parse error: sheet=" + cell.getSheet().getSheetName() 
					+ ",row=" + cell.getRowIndex() + ",column=" + cell.getColumnIndex());
			return "";
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
		IoUtil.close(inputStream);
	}

	/**
	 * 
	 * @param sheetRangeIndex sheetRangeIndex
	 * @param sheetName sheetName
	 * @return boolean
	 */
	protected boolean shouldSheetProcess(int sheetRangeIndex, String sheetName) {
		return true;
	}
	
	/**
	 * sheet重新排序
	 * @param sheetRangeList 原始sheet顺序
	 * @return 重排序后的sheetlist
	 */
	protected List<String> reRangeSheet(List<String> sheetRangeList) {
		return sheetRangeList;
	}
}
