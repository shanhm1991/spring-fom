package org.eto.fom.util.file.reader;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author shanhm
 *
 */
class EventHandler extends DefaultHandler {
	
	private int sheetIndex;

	private SharedStringsTable sst;

	private int rowMax;

	private int cellMax;

	private int rowIndex = 0;

	private int cellIndex = 0;

	private boolean isInSharedTable;

	private String lastContents = ""; 
	
	private List<String> columnList;
	
	private List<ExcelRow> sheetData;
	
	public List<ExcelRow> getSheetData() {
		return sheetData;
	}

	public EventHandler(SharedStringsTable sst) {
		this.sst = sst;
	}
	
	public int getSheetIndex() {
		return sheetIndex;
	}

	public void setSheetIndex(int sheetIndex) {
		this.sheetIndex = sheetIndex;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("dimension")) {
			String dimension = attributes.getValue("ref");
			if(dimension.contains(":")){
				dimension = dimension.split(":")[1];
			}
			rowMax = getRowIndex(dimension);  
			cellMax = getCellIndex(dimension);
			sheetData = new ArrayList<>(rowMax);
		}

		if (qName.equals("row")) {
			int currentRow = Integer.valueOf(attributes.getValue("r")) - 1;
			while(rowIndex < currentRow - 1){
				rowIndex++;
				ExcelRow data = new ExcelRow(rowIndex, new ArrayList<String>(0));
				data.setSheetIndex(sheetIndex); 
				//data.setSheetName(sheetName); 
				data.setEmpty(true); 
				data.setLastRow(rowIndex == rowMax - 1); 
				sheetData.add(data);
			}
			rowIndex = currentRow;
			columnList = new ArrayList<>(cellMax);
		}

		if(qName.equals("c")) {
			int currentCell = getCellIndex(attributes.getValue("r"));
			while(cellIndex < currentCell){
				columnList.add("");
				cellIndex++;
			}
			cellIndex = currentCell;
			isInSharedTable = "s".equals(attributes.getValue("t"));
		}
	}

	@SuppressWarnings("deprecation")
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equals("v")) {
			if (isInSharedTable) {
				int sharedIndex = Integer.valueOf(lastContents);
				lastContents = new XSSFRichTextString(sst.getEntryAt(sharedIndex)).toString();
			}else{
				//TODO
			}
		}

		if (qName.equals("c")) {
			columnList.add(lastContents);
			lastContents = "";
		}

		if (qName.equals("row")) {
			ExcelRow data = new ExcelRow(rowIndex, columnList);
			data.setSheetIndex(sheetIndex); 
			//data.setSheetName(sheetName); 
			data.setEmpty(false); 
			data.setLastRow(rowIndex == rowMax - 1); 
			sheetData.add(data);
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		lastContents += new String(ch, start, length);
	}

	private int getRowIndex(String dimension){
		int len = dimension.length();
		int index = 0;
		for(int i = len - 1; i >= 0; i--){
			if(dimension.charAt(i) > 64){//A
				index = i;
				break;
			}
		}
		return Integer.valueOf(dimension.substring(index + 1));
	}

	private int getCellIndex(String dimension){
		int len = dimension.length();
		int index = 0;
		for(int i = len - 1; i >= 0; i--){
			if(dimension.charAt(i) > 64){//A
				index = i;
				break;
			}
		}
		String cellstr = dimension.substring(0, index + 1);
		
		int cellIndex = 0;
		int indexLen = cellstr.length();
		int bitIndex = 0;
		for(int i = indexLen - 1; i >= 0; i--){ 
			int base = 1;
			int c = cellstr.charAt(i) - 65;
			if(bitIndex > 0){
				c++;//相当于26进制，个位A当做0，进位A当做1
				for(int j = 0; j < bitIndex; j++){
					base *=  26;
				}
			}
			cellIndex += c * base;
			bitIndex++;
		}
		return cellIndex;
	}
}
