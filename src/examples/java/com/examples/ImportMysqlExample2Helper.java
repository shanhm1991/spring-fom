package com.examples;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fom.pool.handler.JdbcHandler;
import com.fom.task.helper.TxtParseHelper;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.task.reader.TextReader;

/**
 * 
 * @author shanhm
 *
 */
public class ImportMysqlExample2Helper implements TxtParseHelper<Map<String, Object>> {

	private static final String POOL = "example_mysql";

	private static final String SQL = 
			"insert into demo(id,name,source,filetype,importway) "
					+ "values (#id#,#name#,#source#,#fileType#,#importWay#)";
	
	private final Logger log;

	public ImportMysqlExample2Helper(String name) {
		log = Logger.getLogger(name);
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	public List<Map<String, Object>> parseRowData(RowData rowData, long batchTime) throws Exception {
		List<String> columns = rowData.getColumnList();
		Map<String,Object> map = new HashMap<>();
		map.put("id", columns.get(0));
		map.put("name", columns.get(1));
		map.put("source", "local");
		map.put("fileType", "txt");
		map.put("importWay", "pool");
		return Arrays.asList(map);
	}
	
	@Override
	public void batchProcess(List<Map<String, Object>> lineDatas, long batchTime) throws Exception {
		JdbcHandler.handler.batchExecute(POOL, SQL, lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
		
	}

	@Override
	public boolean delete(String sourceUri) {
		return new File(sourceUri).delete();
	}

	@Override
	public long getSourceSize(String sourceUri) {
		return new File(sourceUri).length();
	}
}
