package com.fom.examples;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fom.pool.handler.EsHandler;
import com.fom.task.helper.TextParseHelper;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.task.reader.TextReader;

/**
 * 
 * @author shanhm
 *
 */
public class ImportEsExampleHelper implements TextParseHelper<Map<String, Object>> {
	
	private static final String POOL = "example_es";
	
	private final String esIndex;
	
	private final String esType;
	
	private final Logger log;
	
	public ImportEsExampleHelper(String name, String esIndex, String esType) {
		this.log = Logger.getLogger(name);
		this.esIndex = esIndex;
		this.esType = esType;
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	public List<Map<String, Object>> parseRowData(RowData rowData, long batchTime) throws Exception {
		List<String> columns = rowData.getColumnList();
		Map<String,Object> map = new HashMap<>();
		map.put("ID", columns.get(0));
		map.put("NAME", columns.get(1)); 
		map.put("SOURCE", "local");
		map.put("FILETYPE", "txt");
		map.put("IMPORTWAY", "pool");
		return Arrays.asList(map);
	}
	
	@Override
	public void batchProcess(List<Map<String, Object>> lineDatas, long batchTime) throws Exception {
		Map<String,Map<String,Object>> map = new HashMap<>();
		for(Map<String, Object> m : lineDatas){
			map.put(String.valueOf(m.get("ID")), m);
		}
		EsHandler.handler.bulkInsert(POOL, esIndex, esType, map); 
		log.info("处理数据入库:" + map.size());
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
