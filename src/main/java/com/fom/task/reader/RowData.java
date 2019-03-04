package com.fom.task.reader;

import java.util.List;

/**
 * 
 * @author shanhm
 *
 */
public class RowData {

	private int rowIndex;

	private List<String> columnList;

	private int sheetIndex;

	private String sheetName;

	private boolean isLastRow;


	public RowData(int rowIndex, List<String> rowData){
		this.rowIndex = rowIndex;
		this.columnList = rowData;
	}

	public int getRowIndex() {
		return rowIndex;
	}

	void setRowIndex(int rowIndex) {
		this.rowIndex = rowIndex;
	}

	public List<String> getColumnList() {
		return columnList;
	}

	void setColumnList(List<String> columnList) { 
		this.columnList = columnList;
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

	public boolean isLastRow() {
		return isLastRow;
	}

	void setLastRow(boolean isLastRow) {
		this.isLastRow = isLastRow;
	}

}
