package com.fom.context.reader;

import java.util.List;

/**
 * 
 * @author shanhm
 *
 */
public class ReadRow {

	private int rowIndex;

	private List<String> columnDataList;

	private int sheetIndex;

	private String sheetName;


	public ReadRow(int rowIndex, List<String> rowData){
		this.rowIndex = rowIndex;
		this.columnDataList = rowData;
	}
	
	public int getRowIndex() {
		return rowIndex;
	}

	void setRowIndex(int rowIndex) {
		this.rowIndex = rowIndex;
	}

	public List<String> getColumnDataList() {
		return columnDataList;
	}

	void setColumnDataList(List<String> columnDataList) { 
		this.columnDataList = columnDataList;
	}

	public int getSheetIndex() {
		return sheetIndex;
	}

	void setSheetIndex(int sheetIndex) {
		this.sheetIndex = sheetIndex;
	}

	public String getSheetName() {
		return sheetName;
	}

	void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}



}
