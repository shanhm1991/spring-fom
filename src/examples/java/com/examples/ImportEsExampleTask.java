package com.examples;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fom.pool.handler.EsHandler;
import com.fom.task.ParseTextTask;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.task.reader.TextReader;

public class ImportEsExampleTask extends ParseTextTask<Map<String, Object>> {
	
	private static final String POOL = "example_es";
	
	private String esIndex;
	
	private String esType;
	
	private File esJson;

	public ImportEsExampleTask(String sourceUri, int batch, String esIndex, String esType, File esJson) {
		super(sourceUri, batch); 
		this.esIndex = esIndex;
		this.esType = esType;
		this.esJson = esJson;
	}

	@Override
	protected boolean beforeExec() throws Exception {
		if(super.beforeExec() && EsHandler.handler.synCreateIndex(POOL, esIndex, esType, esJson)){
			log.info("创建ES索引[index=" + "demo" + ", type=" + "demo" + "]");
			return true;
		}
		return false;
	}

	@Override
	protected Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	protected List<Map<String, Object>> parseRowData(RowData rowData, long batchTime) throws Exception {
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
	protected void batchProcess(List<Map<String, Object>> batchData, long batchTime) throws Exception {
		Map<String,Map<String,Object>> map = new HashMap<>();
		for(Map<String, Object> m : batchData){
			map.put(String.valueOf(m.get("ID")), m);
		}
		EsHandler.handler.bulkInsert(POOL, esIndex, esType, map); 
		log.info("处理数据入库:" + map.size());
	}

}
