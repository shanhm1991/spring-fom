package qwe;

import java.util.List;

public interface IExcelRowReader {

	void getRows(int sheetIndex, int curRow, List<String> rowlist);  
}
