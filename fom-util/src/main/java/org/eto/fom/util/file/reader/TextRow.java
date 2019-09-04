package org.eto.fom.util.file.reader;

import java.util.List;

/**
 * 
 * @author shanhm
 *
 */
public class TextRow implements IRow {
	
	private int rowIndex;

	private List<String> columnList;
	
	private boolean isEmpty;
	

	public TextRow(int rowIndex, List<String> rowData){
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
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> getColumnList() {
		return columnList;
	}

	@Override
	public String toString() {
		return "{rowIndex=" + rowIndex + ", columnList=" + columnList + ", isEmpty=" + isEmpty + "}";
	}
	
}
