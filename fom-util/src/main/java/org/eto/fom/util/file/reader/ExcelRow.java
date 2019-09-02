package org.eto.fom.util.file.reader;

import java.util.List;

/**
 * 
 * @author shanhm
 *
 */
public class ExcelRow implements ReaderRow{

	private int rowIndex;

	private List<String> columnList;

	private int sheetIndex;

	private String sheetName;

	private boolean isLastRow;

	private boolean isEmpty;


	public ExcelRow(int rowIndex, List<String> rowData){
		this.rowIndex = rowIndex;
		this.columnList = rowData;
	}

	@Override
	public int getRowIndex() {
		return rowIndex;
	}

	@Override
	public boolean isEmpty() {
		return isEmpty;
	}

	void setEmpty(boolean isEmpty) {
		this.isEmpty = isEmpty;
	}

	@Override
	public boolean isLastRow() {
		return isLastRow;
	}

	void setLastRow(boolean isLastRow) {
		this.isLastRow = isLastRow;
	}

	@Override
	public List<String> getColumnList() {
		return columnList;
	}

	@Override
	public int getSheetIndex() {
		return sheetIndex;
	}

	void setSheetIndex(int sheetIndex) {
		this.sheetIndex = sheetIndex;
	}

	@Override
	public String getSheetName() {
		return sheetName;
	}

	void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}

	@Override
	public String toString() {
		return "{rowIndex=" + rowIndex + ", columnList=" + columnList + ", sheetIndex=" + sheetIndex
				+ ", sheetName=" + sheetName + ", isLastRow=" + isLastRow + ", isEmpty=" + isEmpty + "}";
	}
}
