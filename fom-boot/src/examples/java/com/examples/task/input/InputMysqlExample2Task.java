package com.examples.task.input;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eto.fom.task.parse.ParseExcelTask;
import org.eto.fom.util.file.reader.ExcelRow;
import org.eto.fom.util.file.reader.IReader;
import org.eto.fom.util.pool.handler.JdbcHandler;

/**
 * 
 * @author shanhm
 *
 */
public class InputMysqlExample2Task extends ParseExcelTask<Map<String, Object>> {

	private static final String POOL = "example_mysql";

	private static final String SQL = 
			"insert into demo(id,name,source,filetype,importway) "
					+ "values (#id#,#name#,#source#,#fileType#,#importWay#)";
	
	public InputMysqlExample2Task(String sourceUri, int batch){
		super(sourceUri, batch, false); 
	}

	@Override
	public List<Map<String, Object>> parseRowData(ExcelRow rowData, long batchTime) throws Exception {
		List<String> columns = rowData.getColumnList();
		Map<String,Object> map = new HashMap<>();
		map.put("id", columns.get(0));
		map.put("name", columns.get(1));
		map.put("source", "local");
		map.put("fileType", "Excel");
		map.put("importWay", "pool");
		return Arrays.asList(map);
	}
	
	@Override
	public void batchProcess(List<Map<String, Object>> lineDatas, long batchTime) throws Exception {
		JdbcHandler.handler.batchExecute(POOL, SQL, lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
		
	}

	@Override
	protected InputStream getExcelInputStream(String sourceUri) throws Exception {
		return new FileInputStream(sourceUri);
	}

	@Override
	protected String getExcelType() {
		return IReader.EXCEL_XLSX;
	}

}
